package com.luno.core.domain.repository

import com.luno.core.model.Conversation
import com.luno.core.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    val conversationsFlow: Flow<List<Conversation>>
    suspend fun fetchConversations()
    suspend fun startConversation(recipientId: String): Result<String>
    fun getMessagesForConversation(convId: String): Flow<List<Message>>
    fun sendMessage(convId: String, text: String)
    fun getConversation(convId: String): Conversation?
}
