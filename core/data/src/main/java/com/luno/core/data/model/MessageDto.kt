package com.luno.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    val content: String,
    val type: String? = "text",
    @SerialName("created_at")
    val createdAt: String? = null
)
