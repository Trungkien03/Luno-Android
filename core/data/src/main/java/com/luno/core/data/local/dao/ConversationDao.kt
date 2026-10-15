package com.luno.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.luno.core.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :convId LIMIT 1")
    suspend fun getConversationById(convId: String): ConversationEntity?

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertConversations(conversations: List<ConversationEntity>)

    @Query("DELETE FROM conversations WHERE id = :convId")
    suspend fun deleteConversation(convId: String)
}
