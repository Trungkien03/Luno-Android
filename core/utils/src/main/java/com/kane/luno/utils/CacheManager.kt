package com.kane.luno.utils

import android.content.Context
import coil.Coil
import java.io.File
import java.util.Locale

object CacheManager {

    fun getFormattedCacheSize(context: Context): String {
        val bytes = getFolderSize(context.cacheDir)
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(
                Locale.US,
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
            )

            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun clearAppCache(context: Context) {
        try {
            deleteFolder(context.cacheDir)
            Coil.imageLoader(context).diskCache?.clear()
            Coil.imageLoader(context).memoryCache?.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }

    private fun deleteFolder(file: File?): Boolean {
        if (file == null || !file.exists()) return false
        file.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteFolder(child)
            } else {
                child.delete()
            }
        }
        return file.delete()
    }
}
