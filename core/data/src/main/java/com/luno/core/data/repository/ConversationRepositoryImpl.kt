package com.luno.core.data.repository

import com.luno.core.domain.repository.ConversationRepository
import com.luno.core.model.Conversation
import com.luno.core.model.Message
import com.luno.core.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ConversationRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : ConversationRepository {

    private val _conversationsFlow = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversationsFlow: Flow<List<Conversation>> = _conversationsFlow.asStateFlow()

    private val _messagesFlowMap = mutableMapOf<String, MutableStateFlow<List<Message>>>()

    private fun parseTimestamp(timestampStr: String?): Long {
        if (timestampStr.isNullOrBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(timestampStr)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    override suspend fun fetchConversations() {
        try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            println("LunoDebug: fetchConversations currentUserId = $currentUserId")
            if (currentUserId == null) return

            val memberships = supabaseClient.postgrest["conversation_members"]
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }.decodeList<JsonObject>()

            println("LunoDebug: memberships count = ${memberships.size}")
            val convIds = memberships.mapNotNull { it["conversation_id"]?.jsonPrimitive?.content }
            if (convIds.isEmpty()) {
                _conversationsFlow.value = emptyList()
                return
            }

            val conversationsData = supabaseClient.postgrest["conversations"]
                .select {
                    filter {
                        isIn("id", convIds)
                    }
                }.decodeList<JsonObject>()

            println("LunoDebug: conversationsData count = ${conversationsData.size}")
            val conversationList = mutableListOf<Conversation>()

            for (convJson in conversationsData) {
                val convId = convJson["id"]?.jsonPrimitive?.content ?: continue
                val type = convJson["type"]?.jsonPrimitive?.contentOrNull ?: "direct"
                val title = convJson["title"]?.jsonPrimitive?.contentOrNull ?: ""
                val avatarUrl = convJson["avatar_url"]?.jsonPrimitive?.contentOrNull ?: ""

                val members = supabaseClient.postgrest["conversation_members"]
                    .select {
                        filter {
                            eq("conversation_id", convId)
                        }
                    }.decodeList<JsonObject>()

                val otherMemberIds = members
                    .mapNotNull { it["user_id"]?.jsonPrimitive?.content }
                    .filter { it != currentUserId }

                var recipient =
                    User(id = "", name = title.ifBlank { "Người dùng" }, avatarUrl = avatarUrl)
                if (type == "direct" && otherMemberIds.isNotEmpty()) {
                    val otherUserId = otherMemberIds.first()
                    val userRecord = supabaseClient.postgrest["users"]
                        .select {
                            filter {
                                eq("id", otherUserId)
                            }
                        }.decodeSingleOrNull<JsonObject>()

                    if (userRecord != null) {
                        val name = userRecord["display_name"]?.jsonPrimitive?.contentOrNull
                            ?: userRecord["name"]?.jsonPrimitive?.contentOrNull
                            ?: userRecord["email"]?.jsonPrimitive?.contentOrNull
                            ?: "Người dùng"
                        val userAvatar =
                            userRecord["avatar_url"]?.jsonPrimitive?.contentOrNull ?: ""
                        recipient = User(id = otherUserId, name = name, avatarUrl = userAvatar)
                    }
                }

                val lastMsgId = convJson["last_message_id"]?.jsonPrimitive?.contentOrNull
                var lastMessage = Message(
                    id = "m_default",
                    senderId = "",
                    text = "Chưa có tin nhắn",
                    timestamp = System.currentTimeMillis(),
                    isRead = true
                )

                if (!lastMsgId.isNullOrBlank()) {
                    val msgRecord = supabaseClient.postgrest["messages"]
                        .select {
                            filter {
                                eq("id", lastMsgId)
                            }
                        }.decodeSingleOrNull<JsonObject>()

                    if (msgRecord != null) {
                        val msgId = msgRecord["id"]?.jsonPrimitive?.content ?: ""
                        val senderId = msgRecord["sender_id"]?.jsonPrimitive?.content ?: ""
                        val content = msgRecord["content"]?.jsonPrimitive?.content ?: ""
                        val createdAt = msgRecord["created_at"]?.jsonPrimitive?.content
                        val timestamp = parseTimestamp(createdAt)
                        lastMessage = Message(
                            id = msgId,
                            senderId = senderId,
                            text = content,
                            timestamp = timestamp,
                            isRead = true
                        )
                    }
                }

                conversationList.add(
                    Conversation(
                        id = convId,
                        recipient = recipient,
                        lastMessage = lastMessage,
                        unreadCount = 0,
                        isPinned = false
                    )
                )
            }

            _conversationsFlow.value = conversationList
        } catch (e: Exception) {
            e.printStackTrace()
            println("LunoDebug: fetchConversations error: ${e.message}")
        }
    }

    override suspend fun startConversation(recipientId: String): Result<String> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            val myConvs = supabaseClient.postgrest["conversation_members"]
                .select {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }.decodeList<JsonObject>()
                .mapNotNull { it["conversation_id"]?.jsonPrimitive?.content }

            if (myConvs.isNotEmpty()) {
                val sharedConvs = supabaseClient.postgrest["conversation_members"]
                    .select {
                        filter {
                            eq("user_id", recipientId)
                            isIn("conversation_id", myConvs)
                        }
                    }.decodeList<JsonObject>()
                    .mapNotNull { it["conversation_id"]?.jsonPrimitive?.content }

                for (convId in sharedConvs) {
                    val conv = supabaseClient.postgrest["conversations"]
                        .select {
                            filter {
                                eq("id", convId)
                                eq("type", "direct")
                            }
                        }.decodeSingleOrNull<JsonObject>()
                    if (conv != null) {
                        return Result.success(convId)
                    }
                }
            }

            // Fetch recipient user info for immediate local caching
            val userRecord = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        eq("id", recipientId)
                    }
                }.decodeSingleOrNull<JsonObject>()

            val recipientName = userRecord?.let {
                it["display_name"]?.jsonPrimitive?.content
                    ?: it["name"]?.jsonPrimitive?.content
                    ?: it["email"]?.jsonPrimitive?.content
                    ?: "Người dùng"
            } ?: "Người dùng"
            val recipientAvatar = userRecord?.get("avatar_url")?.jsonPrimitive?.content ?: ""
            val recipient =
                User(id = recipientId, name = recipientName, avatarUrl = recipientAvatar)

            val newConvJson = supabaseClient.postgrest["conversations"]
                .insert(
                    JsonObject(mapOf("type" to JsonPrimitive("direct")))
                ) {
                    select()
                }.decodeSingle<JsonObject>()

            val newConvId = newConvJson["id"]?.jsonPrimitive?.content
                ?: return Result.failure(Exception("Failed to create conversation"))

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

            val newConversation = Conversation(
                id = newConvId,
                recipient = recipient,
                lastMessage = Message(
                    id = "m_default",
                    senderId = "",
                    text = "Chưa có tin nhắn",
                    timestamp = System.currentTimeMillis(),
                    isRead = true
                ),
                unreadCount = 0,
                isPinned = false
            )

            val currentList = _conversationsFlow.value.toMutableList()
            if (currentList.none { it.id == newConvId }) {
                currentList.add(0, newConversation)
                _conversationsFlow.value = currentList
            }

            Result.success(newConvId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getConversationDetails(convId: String): Conversation? {
        getConversation(convId)?.let { return it }
        return try {
            val currentUserId =
                supabaseClient.auth.currentUserOrNull()?.id ?: return fallbackConversation(convId)

            val convJson = supabaseClient.postgrest["conversations"]
                .select {
                    filter {
                        eq("id", convId)
                    }
                }.decodeSingleOrNull<JsonObject>() ?: return fallbackConversation(convId)

            val type = convJson["type"]?.jsonPrimitive?.contentOrNull ?: "direct"
            val title = convJson["title"]?.jsonPrimitive?.contentOrNull ?: ""
            val avatarUrl = convJson["avatar_url"]?.jsonPrimitive?.contentOrNull ?: ""

            val members = supabaseClient.postgrest["conversation_members"]
                .select {
                    filter {
                        eq("conversation_id", convId)
                    }
                }.decodeList<JsonObject>()

            val otherMemberIds = members
                .mapNotNull { it["user_id"]?.jsonPrimitive?.content }
                .filter { it != currentUserId }

            var recipient =
                User(id = "", name = title.ifBlank { "Người dùng" }, avatarUrl = avatarUrl)
            if (type == "direct" && otherMemberIds.isNotEmpty()) {
                val otherUserId = otherMemberIds.first()
                val userRecord = supabaseClient.postgrest["users"]
                    .select {
                        filter {
                            eq("id", otherUserId)
                        }
                    }.decodeSingleOrNull<JsonObject>()

                if (userRecord != null) {
                    val name = userRecord["display_name"]?.jsonPrimitive?.contentOrNull
                        ?: userRecord["name"]?.jsonPrimitive?.contentOrNull
                        ?: userRecord["email"]?.jsonPrimitive?.contentOrNull
                        ?: "Người dùng"
                    val userAvatar = userRecord["avatar_url"]?.jsonPrimitive?.contentOrNull ?: ""
                    recipient = User(id = otherUserId, name = name, avatarUrl = userAvatar)
                }
            }

            val lastMsgId = convJson["last_message_id"]?.jsonPrimitive?.contentOrNull
            var lastMessage = Message(
                id = "m_default",
                senderId = "",
                text = "Chưa có tin nhắn",
                timestamp = System.currentTimeMillis(),
                isRead = true
            )

            if (!lastMsgId.isNullOrBlank()) {
                val msgRecord = supabaseClient.postgrest["messages"]
                    .select {
                        filter {
                            eq("id", lastMsgId)
                        }
                    }.decodeSingleOrNull<JsonObject>()

                if (msgRecord != null) {
                    val msgId = msgRecord["id"]?.jsonPrimitive?.content ?: ""
                    val senderId = msgRecord["sender_id"]?.jsonPrimitive?.content ?: ""
                    val content = msgRecord["content"]?.jsonPrimitive?.content ?: ""
                    val createdAt = msgRecord["created_at"]?.jsonPrimitive?.content
                    val timestamp = parseTimestamp(createdAt)
                    lastMessage = Message(
                        id = msgId,
                        senderId = senderId,
                        text = content,
                        timestamp = timestamp,
                        isRead = true
                    )
                }
            }

            val conversation = Conversation(
                id = convId,
                recipient = recipient,
                lastMessage = lastMessage,
                unreadCount = 0,
                isPinned = false
            )

            val currentList = _conversationsFlow.value.toMutableList()
            if (currentList.none { it.id == convId }) {
                currentList.add(conversation)
                _conversationsFlow.value = currentList
            }

            conversation
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackConversation(convId)
        }
    }

    private fun fallbackConversation(convId: String): Conversation {
        return Conversation(
            id = convId,
            recipient = User(id = "", name = "Cuộc trò chuyện", avatarUrl = ""),
            lastMessage = Message(
                id = "m_default",
                senderId = "",
                text = "Chưa có tin nhắn",
                timestamp = System.currentTimeMillis(),
                isRead = true
            ),
            unreadCount = 0,
            isPinned = false
        )
    }

    override fun getMessagesForConversation(convId: String): Flow<List<Message>> {
        if (!_messagesFlowMap.containsKey(convId)) {
            _messagesFlowMap[convId] = MutableStateFlow(emptyList())
            CoroutineScope(Dispatchers.IO).launch {
                fetchMessages(convId)
            }
        }
        return _messagesFlowMap[convId]!!.asStateFlow()
    }

    private suspend fun fetchMessages(convId: String) {
        try {
            val messagesData = supabaseClient.postgrest["messages"]
                .select {
                    filter {
                        eq("conversation_id", convId)
                    }
                }.decodeList<JsonObject>()

            val messages = messagesData.map { msg ->
                val id = msg["id"]?.jsonPrimitive?.content ?: ""
                val senderId = msg["sender_id"]?.jsonPrimitive?.content ?: ""
                val content = msg["content"]?.jsonPrimitive?.content ?: ""
                val createdAt = msg["created_at"]?.jsonPrimitive?.content
                val timestamp = parseTimestamp(createdAt)
                Message(
                    id = id,
                    senderId = senderId,
                    text = content,
                    timestamp = timestamp,
                    isRead = true
                )
            }.sortedBy { it.timestamp }

            _messagesFlowMap[convId]?.value = messages
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun sendMessage(convId: String, text: String) {
        if (text.isBlank()) return
        val currentUserId = supabaseClient.auth.currentUserOrNull()?.id ?: "me"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newMsgJson = supabaseClient.postgrest["messages"]
                    .insert(
                        JsonObject(
                            mapOf(
                                "conversation_id" to JsonPrimitive(convId),
                                "sender_id" to JsonPrimitive(currentUserId),
                                "content" to JsonPrimitive(text),
                                "type" to JsonPrimitive("text")
                            )
                        )
                    ) {
                        select()
                    }.decodeSingle<JsonObject>()

                val msgId =
                    newMsgJson["id"]?.jsonPrimitive?.content ?: "msg_${System.currentTimeMillis()}"
                val createdAt = newMsgJson["created_at"]?.jsonPrimitive?.content
                val timestamp = parseTimestamp(createdAt)

                val newMessage = Message(
                    id = msgId,
                    senderId = currentUserId,
                    text = text,
                    timestamp = timestamp,
                    isRead = true
                )

                val list = _messagesFlowMap[convId]?.value.orEmpty().toMutableList()
                list.add(newMessage)
                _messagesFlowMap[convId]?.value = list.toList()

                val currentConvs = _conversationsFlow.value.toMutableList()
                val index = currentConvs.indexOfFirst { it.id == convId }
                if (index != -1) {
                    val conv = currentConvs[index]
                    currentConvs[index] = conv.copy(lastMessage = newMessage, unreadCount = 0)
                    _conversationsFlow.value = currentConvs
                }

                supabaseClient.postgrest["conversations"]
                    .update(
                        JsonObject(mapOf("last_message_id" to JsonPrimitive(msgId)))
                    ) {
                        filter {
                            eq("id", convId)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getConversation(convId: String): Conversation? {
        return _conversationsFlow.value.find { it.id == convId }
    }

    override fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
}
