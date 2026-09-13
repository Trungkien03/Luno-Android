package com.luno.core.model

data class Conversation(
    val id: String,
    val recipient: User,
    val lastMessage: Message,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false
)
