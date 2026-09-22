package com.luno.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("conversation_type")
    val conversationType: String,
    @SerialName("conversation_title")
    val conversationTitle: String? = null,
    @SerialName("conversation_avatar")
    val conversationAvatar: String? = null,
    @SerialName("recipient_id")
    val recipientId: String? = null,
    @SerialName("recipient_name")
    val recipientName: String? = null,
    @SerialName("recipient_avatar")
    val recipientAvatar: String? = null,
    @SerialName("last_message_id")
    val lastMessageId: String? = null,
    @SerialName("last_message_raw_text")
    val lastMessageRawText: String? = null,
    @SerialName("last_message_type")
    val lastMessageType: String? = null,
    @SerialName("last_message_status_key")
    val lastMessageStatusKey: String? = null,
    @SerialName("last_message_time")
    val lastMessageTime: String? = null,
    @SerialName("unread_count")
    val unreadCount: Int = 0
)
