package com.luno.core.data.datasource

import com.luno.core.data.model.MessageDto
import com.luno.core.data.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ConversationRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    fun getCurrentUserId(): String? = supabaseClient.auth.currentUserOrNull()?.id

    suspend fun fetchMemberships(userId: String): List<String> {
        val memberships = supabaseClient.postgrest["conversation_members"]
            .select {
                filter { eq("user_id", userId) }
            }.decodeList<JsonObject>()
        return memberships.mapNotNull { it["conversation_id"]?.jsonPrimitive?.content }
    }

    suspend fun fetchConversationsData(convIds: List<String>): List<JsonObject> {
        return supabaseClient.postgrest["conversations"]
            .select {
                filter { isIn("id", convIds) }
            }.decodeList<JsonObject>()
    }

    suspend fun fetchConversationMembers(convId: String): List<JsonObject> {
        return supabaseClient.postgrest["conversation_members"]
            .select {
                filter { eq("conversation_id", convId) }
            }.decodeList<JsonObject>()
    }

    suspend fun fetchUserRecord(userId: String): UserDto? {
        return supabaseClient.postgrest["users"]
            .select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<UserDto>()
    }

    suspend fun fetchMessageRecord(msgId: String): MessageDto? {
        return supabaseClient.postgrest["messages"]
            .select {
                filter { eq("id", msgId) }
            }.decodeSingleOrNull<MessageDto>()
    }

    suspend fun findExistingDirectConversation(
        currentUserId: String,
        recipientId: String
    ): String? {
        val myConvs = fetchMemberships(currentUserId)
        if (myConvs.isEmpty()) return null

        val sharedMemberships = supabaseClient.postgrest["conversation_members"]
            .select {
                filter {
                    eq("user_id", recipientId)
                    isIn("conversation_id", myConvs)
                }
            }.decodeList<JsonObject>()

        val sharedConvIds =
            sharedMemberships.mapNotNull { it["conversation_id"]?.jsonPrimitive?.content }

        for (convId in sharedConvIds) {
            val conv = supabaseClient.postgrest["conversations"]
                .select {
                    filter {
                        eq("id", convId)
                        eq("type", "direct")
                    }
                }.decodeSingleOrNull<JsonObject>()
            if (conv != null) {
                return convId
            }
        }
        return null
    }

    suspend fun createDirectConversation(currentUserId: String, recipientId: String): String {
        val newConvId = UUID.randomUUID().toString()
        supabaseClient.postgrest["conversations"]
            .insert(
                JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(newConvId),
                        "type" to JsonPrimitive("direct")
                    )
                )
            )

        supabaseClient.postgrest["conversation_members"]
            .insert(
                listOf(
                    JsonObject(
                        mapOf(
                            "conversation_id" to JsonPrimitive(newConvId),
                            "user_id" to JsonPrimitive(currentUserId)
                        )
                    ),
                    JsonObject(
                        mapOf(
                            "conversation_id" to JsonPrimitive(newConvId),
                            "user_id" to JsonPrimitive(recipientId)
                        )
                    )
                )
            )

        return newConvId
    }

    suspend fun createGroupConversation(title: String, memberIds: List<String>): String {
        val newConvId = UUID.randomUUID().toString()
        supabaseClient.postgrest["conversations"]
            .insert(
                JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(newConvId),
                        "type" to JsonPrimitive("group"),
                        "title" to JsonPrimitive(title)
                    )
                )
            )

        val memberList = memberIds.map { memberId ->
            JsonObject(
                mapOf(
                    "conversation_id" to JsonPrimitive(newConvId),
                    "user_id" to JsonPrimitive(memberId)
                )
            )
        }
        supabaseClient.postgrest["conversation_members"].insert(memberList)

        return newConvId
    }

    suspend fun fetchMessages(convId: String, limit: Long = 20): List<MessageDto> {
        val list = supabaseClient.postgrest["messages"]
            .select {
                filter { eq("conversation_id", convId) }
                order("created_at", Order.DESCENDING)
                limit(limit)
            }.decodeList<MessageDto>()
        return list.reversed()
    }

    suspend fun fetchOlderMessages(
        convId: String,
        beforeCreatedAt: String,
        limit: Long = 20
    ): List<MessageDto> {
        val list = supabaseClient.postgrest["messages"]
            .select {
                filter {
                    eq("conversation_id", convId)
                    lt("created_at", beforeCreatedAt)
                }
                order("created_at", Order.DESCENDING)
                limit(limit)
            }.decodeList<MessageDto>()
        return list.reversed()
    }

    suspend fun markMessagesAsReadRecord(convId: String, currentUserId: String) {
        supabaseClient.postgrest["messages"]
            .update(JsonObject(mapOf("is_read" to JsonPrimitive(true)))) {
                filter {
                    eq("conversation_id", convId)
                    neq("sender_id", currentUserId)
                    eq("is_read", false)
                }
            }
    }

    suspend fun sendMessageRecord(
        convId: String,
        senderId: String,
        text: String,
        mediaUrl: String? = null,
        mediaType: String = "TEXT",
        fileName: String? = null,
        fileSize: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null
    ): MessageDto {
        val jsonMap = mutableMapOf<String, JsonPrimitive>(
            "conversation_id" to JsonPrimitive(convId),
            "sender_id" to JsonPrimitive(senderId),
            "content" to JsonPrimitive(text),
            "media_type" to JsonPrimitive(mediaType)
        )
        if (!mediaUrl.isNullOrBlank()) jsonMap["media_url"] = JsonPrimitive(mediaUrl)
        if (!fileName.isNullOrBlank()) jsonMap["file_name"] = JsonPrimitive(fileName)
        if (!fileSize.isNullOrBlank()) jsonMap["file_size"] = JsonPrimitive(fileSize)
        if (!replyToId.isNullOrBlank()) jsonMap["reply_to_id"] = JsonPrimitive(replyToId)
        if (!replyToText.isNullOrBlank()) jsonMap["reply_to_text"] = JsonPrimitive(replyToText)
        if (!replyToSender.isNullOrBlank()) jsonMap["reply_to_sender"] =
            JsonPrimitive(replyToSender)

        return supabaseClient.postgrest["messages"]
            .insert(JsonObject(jsonMap)) {
                select()
            }.decodeSingle<MessageDto>()
    }

    suspend fun unsendMessageRecord(messageId: String) {
        supabaseClient.postgrest["messages"]
            .update(
                JsonObject(
                    mapOf(
                        "content" to JsonPrimitive("Tin nhắn đã được thu hồi"),
                        "is_unsent" to JsonPrimitive(true),
                        "media_url" to JsonNull,
                        "file_name" to JsonNull,
                        "file_size" to JsonNull
                    )
                )
            ) {
                filter { eq("id", messageId) }
            }
    }

    suspend fun updateLastMessageId(convId: String, msgId: String) {
        supabaseClient.postgrest["conversations"]
            .update(
                JsonObject(mapOf("last_message_id" to JsonPrimitive(msgId)))
            ) {
                filter { eq("id", convId) }
            }
    }

    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val nowStr = dateFormat.format(Date())
        supabaseClient.postgrest["users"].update(
            JsonObject(
                mapOf(
                    "is_online" to JsonPrimitive(isOnline),
                    "last_seen_at" to JsonPrimitive(nowStr)
                )
            )
        ) {
            filter { eq("id", userId) }
        }
    }

    fun createMessageChannel(convId: String): RealtimeChannel =
        supabaseClient.channel("messages:$convId")

    fun createUserStatusChannel(): RealtimeChannel = supabaseClient.channel("user-status-global")
}
