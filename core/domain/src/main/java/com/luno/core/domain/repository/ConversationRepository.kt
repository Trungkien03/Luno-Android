package com.luno.core.domain.repository

import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    val conversationsFlow: Flow<List<Conversation>>
    suspend fun fetchConversations()
    suspend fun startConversation(recipientId: String): Result<String>
    suspend fun getConversationDetails(convId: String): Conversation?
    fun getMessagesForConversation(convId: String): Flow<List<Message>>
    suspend fun loadOlderMessages(convId: String)
    fun getTypingStatus(convId: String): Flow<Boolean>
    suspend fun sendTypingStatus(convId: String, isTyping: Boolean)
    fun stopObservingMessages(convId: String)
    suspend fun sendMessage(convId: String, text: String): Result<Unit>
    suspend fun sendMediaMessage(
        convId: String,
        text: String,
        mediaUrl: String,
        mediaType: MediaType,
        fileName: String? = null,
        fileSize: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ): Result<Unit>

    suspend fun unsendMessage(convId: String, messageId: String): Result<Unit>
    fun getConversation(convId: String): Conversation?
    fun getCurrentUserId(): String?
    suspend fun updateOnlineStatus(isOnline: Boolean)
}
