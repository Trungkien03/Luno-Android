package com.luno.core.data.repository

import com.luno.core.data.datasource.ConversationRemoteDataSource
import com.luno.core.data.mapper.MessageMapper
import com.luno.core.data.mapper.UserMapper
import com.luno.core.data.model.MessageDto
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.Message
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.ConversationRepository
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class ConversationRepositoryImpl(
    private val remoteDataSource: ConversationRemoteDataSource
) : ConversationRepository {

    private val _conversationsFlow = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversationsFlow: Flow<List<Conversation>> = _conversationsFlow.asStateFlow()

    private val _messagesFlowMap = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val _messageJobs = mutableMapOf<String, Job>()
    private val _messageChannels = mutableMapOf<String, RealtimeChannel>()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var userStatusChannelJob: Job? = null

    override suspend fun fetchConversations() {
        try {
            val currentUserId = remoteDataSource.getCurrentUserId() ?: return
            val convIds = remoteDataSource.fetchMemberships(currentUserId)
            if (convIds.isEmpty()) {
                _conversationsFlow.value = emptyList()
                subscribeToUserStatus()
                return
            }

            val conversationsData = remoteDataSource.fetchConversationsData(convIds)
            val conversationList = mutableListOf<Conversation>()

            for (convJson in conversationsData) {
                val convId = convJson["id"]?.jsonPrimitive?.content ?: continue
                val type = convJson["type"]?.jsonPrimitive?.content ?: "direct"
                val title = convJson["title"]?.jsonPrimitive?.content ?: ""
                val avatarUrl = convJson["avatar_url"]?.jsonPrimitive?.content ?: ""

                val members = remoteDataSource.fetchConversationMembers(convId)
                val otherMemberIds = members
                    .mapNotNull { it["user_id"]?.jsonPrimitive?.content }
                    .filter { it != currentUserId }

                var recipient =
                    User(id = "", name = title.ifBlank { "Người dùng" }, avatarUrl = avatarUrl)
                if (type == "direct" && otherMemberIds.isNotEmpty()) {
                    val otherUserId = otherMemberIds.first()
                    val userDto = remoteDataSource.fetchUserRecord(otherUserId)
                    if (userDto != null) {
                        recipient = UserMapper.mapToDomain(userDto)
                    }
                }

                val lastMsgId = convJson["last_message_id"]?.jsonPrimitive?.content
                var lastMessage = Message(
                    id = "m_default",
                    senderId = "",
                    text = "Chưa có tin nhắn",
                    timestamp = System.currentTimeMillis(),
                    isRead = true
                )

                if (!lastMsgId.isNullOrBlank()) {
                    val msgDto = remoteDataSource.fetchMessageRecord(lastMsgId)
                    if (msgDto != null) {
                        lastMessage = MessageMapper.mapToDomain(msgDto)
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
            subscribeToUserStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun subscribeToUserStatus() {
        if (userStatusChannelJob != null) return
        try {
            val channel = remoteDataSource.createUserStatusChannel()
            userStatusChannelJob =
                channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "users"
                }.onEach { update ->
                    val record = update.record
                    val userId = record["id"]?.jsonPrimitive?.content ?: return@onEach
                    val isOnline = record["is_online"]?.jsonPrimitive?.content?.toBoolean() ?: false
                    val lastSeenAt = record["last_seen_at"]?.jsonPrimitive?.content

                    val currentConvs = _conversationsFlow.value.toMutableList()
                    var updated = false
                    for (i in currentConvs.indices) {
                        if (currentConvs[i].recipient.id == userId) {
                            val recipient = currentConvs[i].recipient.copy(
                                isOnline = isOnline,
                                lastSeenAt = lastSeenAt
                            )
                            currentConvs[i] = currentConvs[i].copy(recipient = recipient)
                            updated = true
                        }
                    }
                    if (updated) {
                        _conversationsFlow.value = currentConvs
                    }
                }.launchIn(repositoryScope)

            channel.subscribe()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateOnlineStatus(isOnline: Boolean) {
        try {
            val currentUserId = remoteDataSource.getCurrentUserId() ?: return
            remoteDataSource.updateOnlineStatus(currentUserId, isOnline)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun startConversation(recipientId: String): Result<String> {
        return try {
            val currentUserId = remoteDataSource.getCurrentUserId()
                ?: return Result.failure(Exception("Not logged in"))

            val existingConvId =
                remoteDataSource.findExistingDirectConversation(currentUserId, recipientId)
            if (existingConvId != null) {
                return Result.success(existingConvId)
            }

            val userDto = remoteDataSource.fetchUserRecord(recipientId)
            val recipient = if (userDto != null) {
                UserMapper.mapToDomain(userDto)
            } else {
                User(id = recipientId, name = "Người dùng", avatarUrl = "")
            }

            val newConvId = remoteDataSource.createDirectConversation(currentUserId, recipientId)

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
                remoteDataSource.getCurrentUserId() ?: return fallbackConversation(convId)

            val conversationsData = remoteDataSource.fetchConversationsData(listOf(convId))
            val convJson = conversationsData.firstOrNull() ?: return fallbackConversation(convId)

            val type = convJson["type"]?.jsonPrimitive?.content ?: "direct"
            val title = convJson["title"]?.jsonPrimitive?.content ?: ""
            val avatarUrl = convJson["avatar_url"]?.jsonPrimitive?.content ?: ""

            val members = remoteDataSource.fetchConversationMembers(convId)
            val otherMemberIds = members
                .mapNotNull { it["user_id"]?.jsonPrimitive?.content }
                .filter { it != currentUserId }

            var recipient =
                User(id = "", name = title.ifBlank { "Người dùng" }, avatarUrl = avatarUrl)
            if (type == "direct" && otherMemberIds.isNotEmpty()) {
                val otherUserId = otherMemberIds.first()
                val userDto = remoteDataSource.fetchUserRecord(otherUserId)
                if (userDto != null) {
                    recipient = UserMapper.mapToDomain(userDto)
                }
            }

            val lastMsgId = convJson["last_message_id"]?.jsonPrimitive?.content
            var lastMessage = Message(
                id = "m_default",
                senderId = "",
                text = "Chưa có tin nhắn",
                timestamp = System.currentTimeMillis(),
                isRead = true
            )

            if (!lastMsgId.isNullOrBlank()) {
                val msgDto = remoteDataSource.fetchMessageRecord(lastMsgId)
                if (msgDto != null) {
                    lastMessage = MessageMapper.mapToDomain(msgDto)
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
            repositoryScope.launch {
                fetchMessages(convId)
                subscribeToMessages(convId)
            }
        }
        return _messagesFlowMap[convId]!!.asStateFlow()
    }

    override fun stopObservingMessages(convId: String) {
        _messageJobs.remove(convId)?.cancel()
        _messagesFlowMap.remove(convId)
        _messageChannels.remove(convId)?.let { channel ->
            repositoryScope.launch { channel.unsubscribe() }
        }
    }

    private suspend fun fetchMessages(convId: String) {
        try {
            val messageDtos = remoteDataSource.fetchMessages(convId)
            val messages =
                messageDtos.map { MessageMapper.mapToDomain(it) }.sortedBy { it.timestamp }
            _messagesFlowMap[convId]?.value = messages
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun subscribeToMessages(convId: String) {
        if (_messageChannels.containsKey(convId)) return
        val channel = remoteDataSource.createMessageChannel(convId)
        _messageChannels[convId] = channel

        _messageJobs[convId] =
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
                filter("conversation_id", FilterOperator.EQ, convId)
            }.onEach { insert ->
                val record = insert.record
                val id = record["id"]?.jsonPrimitive?.content ?: return@onEach
                val senderId = record["sender_id"]?.jsonPrimitive?.content ?: ""
                val content = record["content"]?.jsonPrimitive?.content ?: ""
                val createdAt = record["created_at"]?.jsonPrimitive?.content

                val messageDto = MessageDto(
                    id = id,
                    conversationId = convId,
                    senderId = senderId,
                    content = content,
                    createdAt = createdAt
                )
                val message = MessageMapper.mapToDomain(messageDto)

                val current = _messagesFlowMap[convId]?.value.orEmpty()
                if (current.none { it.id == id }) {
                    _messagesFlowMap[convId]?.value = (current + message).sortedBy { it.timestamp }
                }

                val convs = _conversationsFlow.value.toMutableList()
                val index = convs.indexOfFirst { it.id == convId }
                if (index != -1) {
                    convs[index] = convs[index].copy(lastMessage = message)
                    _conversationsFlow.value = convs
                }
            }.launchIn(repositoryScope)

        channel.subscribe()
    }

    override suspend fun sendMessage(convId: String, text: String): Result<Unit> {
        if (text.isBlank()) return Result.failure(Exception("Message is blank"))
        val currentUserId = remoteDataSource.getCurrentUserId() ?: "me"

        return try {
            val msgDto = remoteDataSource.sendMessageRecord(convId, currentUserId, text)
            val message = MessageMapper.mapToDomain(msgDto)

            val list = _messagesFlowMap[convId]?.value.orEmpty().toMutableList()
            if (list.none { it.id == message.id }) {
                list.add(message)
                _messagesFlowMap[convId]?.value = list.toList()
            }

            val currentConvs = _conversationsFlow.value.toMutableList()
            val index = currentConvs.indexOfFirst { it.id == convId }
            if (index != -1) {
                val conv = currentConvs[index]
                currentConvs[index] = conv.copy(lastMessage = message, unreadCount = 0)
                _conversationsFlow.value = currentConvs
            }

            remoteDataSource.updateLastMessageId(convId, message.id)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun getConversation(convId: String): Conversation? {
        return _conversationsFlow.value.find { it.id == convId }
    }

    override fun getCurrentUserId(): String? {
        return remoteDataSource.getCurrentUserId()
    }
}
