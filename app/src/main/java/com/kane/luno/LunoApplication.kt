package com.kane.luno

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kane.luno.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LunoApplication : Application(), DefaultLifecycleObserver, ImageLoaderFactory {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super<Application>.onCreate()
        AppModule.initialize(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        applicationScope.launch {
            AppModule.conversationRepository.updateOnlineStatus(true)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        applicationScope.launch {
            AppModule.conversationRepository.updateOnlineStatus(false)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Dành tối đa 25% RAM ứng dụng cho ảnh
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Dành 50MB cho bộ nhớ ổ đĩa
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // Đảm bảo luôn ưu tiên cache kể cả khi server gửi header no-cache
            .build()
    }
}
