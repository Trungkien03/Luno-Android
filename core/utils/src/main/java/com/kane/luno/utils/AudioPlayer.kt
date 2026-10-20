package com.kane.luno.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.net.toUri

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var _isPlaying: Boolean = false
    val isPlaying: Boolean get() = _isPlaying

    fun play(url: String, onCompletion: () -> Unit = {}) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, url.toUri())
                prepareAsync()
                setOnPreparedListener {
                    start()
                    _isPlaying = true
                }
                setOnCompletionListener {
                    _isPlaying = false
                    onCompletion()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying = false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
