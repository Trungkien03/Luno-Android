package com.luno.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: String = "SENT",
    val isRead: Boolean = true
)
