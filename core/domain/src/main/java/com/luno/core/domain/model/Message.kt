package com.luno.core.domain.model

enum class MessageStatus {
    PENDING,   // 🕒 Clock icon (Sending / Offline)
    SENT,      // ✓ Single checkmark
    DELIVERED, // ✓✓ Double checkmark
    READ       // ✓✓ Blue double checkmark
}

data class Message(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean = true,
    val status: MessageStatus = MessageStatus.SENT
)
