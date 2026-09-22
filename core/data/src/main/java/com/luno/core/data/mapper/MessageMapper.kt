package com.luno.core.data.mapper

import com.luno.core.data.model.MessageDto
import com.luno.core.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MessageMapper {
    private val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    fun parseTimestamp(timestampStr: String?): Long {
        if (timestampStr.isNullOrBlank()) return System.currentTimeMillis()
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
        return System.currentTimeMillis()
    }

    fun mapToDomain(dto: MessageDto): Message {
        return Message(
            id = dto.id,
            senderId = dto.senderId,
            text = dto.content,
            timestamp = parseTimestamp(dto.createdAt),
            isRead = true
        )
    }
}
