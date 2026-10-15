package com.luno.core.data.mapper

import com.luno.core.data.model.MessageDto
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.model.Message
import com.luno.core.domain.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MessageMapper {

    fun mapToDomain(dto: MessageDto): Message {
        val parsedTime = parseTimestamp(dto.createdAt)
        val parsedType = try {
            MediaType.valueOf(dto.mediaType?.uppercase() ?: "TEXT")
        } catch (_: Exception) {
            MediaType.TEXT
        }

        return Message(
            id = dto.id,
            senderId = dto.senderId,
            text = dto.content,
            timestamp = parsedTime,
            isRead = true,
            status = MessageStatus.SENT,
            mediaUrl = dto.mediaUrl,
            mediaType = parsedType,
            fileName = dto.fileName,
            fileSize = dto.fileSize,
            replyToMessageId = dto.replyToId,
            replyToText = dto.replyToText,
            replyToSenderName = dto.replyToSender,
            isUnsent = dto.isUnsent ?: false
        )
    }

    private fun parseTimestamp(createdAt: String?): Long {
        if (createdAt.isNullOrBlank()) return System.currentTimeMillis()

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
                val date = sdf.parse(createdAt)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }
}
