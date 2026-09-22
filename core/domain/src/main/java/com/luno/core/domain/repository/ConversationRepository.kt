package com.luno.core.domain.repository

import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    val conversationsFlow: Flow<List<Conversation>>
    suspend fun fetchConversations()
    suspend fun startConversation(recipientId: String): Result<String>
    suspend fun getConversationDetails(convId: String): Conversation?
    fun getMessagesForConversation(convId: String): Flow<List<Message>>
    fun stopObservingMessages(convId: String)
    suspend fun sendMessage(convId: String, text: String): Result<Unit>
    fun getConversation(convId: String): Conversation?
    fun getCurrentUserId(): String?
    suspend fun updateOnlineStatus(isOnline: Boolean)
}
