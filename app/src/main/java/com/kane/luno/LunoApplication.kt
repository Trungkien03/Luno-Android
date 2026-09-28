package com.kane.luno

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.messaging.FirebaseMessaging
import com.kane.luno.di.AppModule
import com.luno.core.network.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LunoApplication : Application(), DefaultLifecycleObserver, ImageLoaderFactory {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super<Application>.onCreate()
        AppModule.initialize(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        applicationScope.launch {
            try {
                AppModule.conversationRepository.updateOnlineStatus(true)
                SupabaseModule.client.realtime.connect()

                FcmSyncHelper.syncToken(applicationScope)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        applicationScope.launch {
            try {
                AppModule.conversationRepository.updateOnlineStatus(false)
                SupabaseModule.client.realtime.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}


object FcmSyncHelper {

    fun syncToken(scope: CoroutineScope) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token.isNullOrBlank()) return@addOnSuccessListener

            scope.launch(Dispatchers.IO) {
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
