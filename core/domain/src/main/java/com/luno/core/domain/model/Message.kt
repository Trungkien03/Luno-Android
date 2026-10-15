package com.luno.core.domain.model

enum class MessageStatus {
    PENDING,   // 🕒 Clock icon (Sending / Offline)
    SENT,      // ✓ Single checkmark
    DELIVERED, // ✓✓ Double checkmark
    READ       // ✓✓ Blue double checkmark
}

enum class MediaType {
    TEXT,
    IMAGE,
    AUDIO,
    FILE
}

data class Message(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean = true,
    val status: MessageStatus = MessageStatus.SENT,
    val mediaUrl: String? = null,
    val mediaType: MediaType = MediaType.TEXT,
    val fileName: String? = null,
    val fileSize: String? = null,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val isUnsent: Boolean = false,
    val reactions: Map<String, Int> = emptyMap()
)
