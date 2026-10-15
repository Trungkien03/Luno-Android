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
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("media_type")
    val mediaType: String? = "TEXT",
    @SerialName("file_name")
    val fileName: String? = null,
    @SerialName("file_size")
    val fileSize: String? = null,
    @SerialName("reply_to_id")
    val replyToId: String? = null,
    @SerialName("reply_to_text")
    val replyToText: String? = null,
    @SerialName("reply_to_sender")
    val replyToSender: String? = null,
    @SerialName("is_unsent")
    val isUnsent: Boolean? = false
)
