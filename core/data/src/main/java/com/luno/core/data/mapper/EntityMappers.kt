package com.luno.core.data.mapper

import com.luno.core.data.local.entity.ConversationEntity
import com.luno.core.data.local.entity.MessageEntity
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.Message
import com.luno.core.domain.model.MessageStatus
import com.luno.core.domain.model.User

object EntityMappers {

    fun ConversationEntity.toDomain(): Conversation {
        return Conversation(
            id = id,
            recipient = User(
                id = recipientId,
                name = recipientName,
                avatarUrl = recipientAvatarUrl,
                isOnline = recipientIsOnline,
                lastSeenAt = recipientLastSeenAt
            ),
            lastMessage = Message(
                id = lastMessageId,
                senderId = "",
                text = lastMessageText,
                timestamp = lastMessageTimestamp
            ),
            unreadCount = unreadCount,
            isPinned = isPinned
        )
    }

    fun Conversation.toEntity(): ConversationEntity {
        return ConversationEntity(
            id = id,
            recipientId = recipient.id,
            recipientName = recipient.name,
            recipientAvatarUrl = recipient.avatarUrl,
            recipientIsOnline = recipient.isOnline,
            recipientLastSeenAt = recipient.lastSeenAt,
            lastMessageId = lastMessage.id,
            lastMessageText = lastMessage.text,
            lastMessageTimestamp = lastMessage.timestamp,
            unreadCount = unreadCount,
            isPinned = isPinned
        )
    }

    fun MessageEntity.toDomain(): Message {
        val messageStatus = try {
            MessageStatus.valueOf(status)
        } catch (_: Exception) {
            MessageStatus.SENT
        }
        return Message(
            id = id,
            senderId = senderId,
            text = content,
            timestamp = timestamp,
            isRead = isRead,
            status = messageStatus
        )
    }

    fun Message.toEntity(conversationId: String): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            content = text,
            timestamp = timestamp,
            status = status.name,
            isRead = isRead
        )
    }
}
