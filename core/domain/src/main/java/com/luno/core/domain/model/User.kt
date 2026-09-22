package com.luno.core.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class User(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null
) {
    val isActuallyOnline: Boolean
        get() {
            if (!isOnline) return false
            if (lastSeenAt.isNullOrBlank()) return false
            val epochMillis = parseUtcEpoch(lastSeenAt) ?: return false
            return (System.currentTimeMillis() - epochMillis) < 90_000
        }

    val lastSeenFormatted: String
        get() {
            if (isActuallyOnline) return "Đang hoạt động"
            if (lastSeenAt.isNullOrBlank()) return "Truy cập gần đây"

            val epochMillis = parseUtcEpoch(lastSeenAt) ?: return "Truy cập gần đây"
            val diffMillis = System.currentTimeMillis() - epochMillis
            if (diffMillis < 0) return "Vừa mới truy cập"

            val diffMinutes = diffMillis / (1000 * 60)
            val diffHours = diffMinutes / 60
            val diffDays = diffHours / 24

            return when {
                diffMinutes < 1 -> "Vừa mới truy cập"
                diffMinutes < 60 -> "Truy cập $diffMinutes phút trước"
                diffHours < 24 -> "Truy cập $diffHours giờ trước"
                diffDays < 7 -> "Truy cập $diffDays ngày trước"
                else -> {
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    "Truy cập " + outputFormat.format(Date(epochMillis))
                }
            }
        }
}

private fun parseUtcEpoch(timestampStr: String): Long? {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(timestampStr)
            if (date != null) return date.time
        } catch (_: Exception) {
        }
    }
    return null
}
