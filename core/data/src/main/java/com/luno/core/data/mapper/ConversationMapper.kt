package com.luno.core.data.mapper

import com.luno.core.data.model.ConversationDto
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.Message
import com.luno.core.domain.model.User
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ConversationMapper {

    fun mapToDomain(dto: ConversationDto): Conversation = with(dto) {
        val isGroup = conversationType == "group"
        val recipientNameStr =
            recipientName ?: if (isGroup) conversationTitle ?: "Nhóm" else "Người dùng"
        val recipientAvatarStr =
            if (isGroup) conversationAvatar.orEmpty() else recipientAvatar.orEmpty()

        val recipient = User(
            id = recipientId ?: "",
            name = recipientNameStr,
            avatarUrl = recipientAvatarStr
        )

        val epochMillis = try {
            if (!lastMessageTime.isNullOrBlank()) {
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ss"
                )
                var time = System.currentTimeMillis()
                for (format in formats) {
                    try {
                        val sdf = SimpleDateFormat(format, Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val date = sdf.parse(lastMessageTime!!)
                        if (date != null) {
                            time = date.time
                            break
                        }
                    } catch (_: Exception) {
                    }
                }
                time
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        val lastMessage = Message(
            id = lastMessageId ?: "",
            senderId = "",
            text = lastMessageRawText ?: "",
            timestamp = epochMillis,
            isRead = true
        )

        return Conversation(
            id = conversationId,
            recipient = recipient,
            lastMessage = lastMessage,
            unreadCount = unreadCount,
            isPinned = false
        )
    }
}
