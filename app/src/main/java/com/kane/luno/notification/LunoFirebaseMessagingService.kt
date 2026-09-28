package com.kane.luno.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kane.luno.MainActivity
import com.kane.luno.R
import com.luno.core.network.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LunoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToSupabase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Tin nhắn mới"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Bạn có một tin nhắn mới"
        val conversationId = remoteMessage.data["conversation_id"]

        showNotification(title, body, conversationId)
    }

    private fun saveTokenToSupabase(token: String) {
        uploadTokenToSupabase(token)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, body: String, conversationId: String?) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Thông báo trò chuyện",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo tin nhắn mới từ Luno"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!conversationId.isNullOrBlank()) {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "chat_notifications"
        const val EXTRA_CONVERSATION_ID = "conversation_id"

        fun syncCurrentFcmToken() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        uploadTokenToSupabase(token)
                    }
                }
            }
        }

        fun uploadTokenToSupabase(token: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentUserId =
                        SupabaseModule.client.auth.currentUserOrNull()?.id ?: return@launch
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val nowStr = dateFormat.format(Date())

                    SupabaseModule.client.postgrest["user_fcm_tokens"].upsert(
                        JsonObject(
                            mapOf(
                                "user_id" to JsonPrimitive(currentUserId),
                                "fcm_token" to JsonPrimitive(token),
                                "device_type" to JsonPrimitive("android"),
                                "updated_at" to JsonPrimitive(nowStr)
                            )
                        )
                    ) {
                        onConflict = "fcm_token"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
