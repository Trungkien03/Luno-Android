package com.luno.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val recipientId: String,
    val recipientName: String,
    val recipientAvatarUrl: String,
    val recipientIsOnline: Boolean,
    val recipientLastSeenAt: String?,
    val lastMessageId: String,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false
)
