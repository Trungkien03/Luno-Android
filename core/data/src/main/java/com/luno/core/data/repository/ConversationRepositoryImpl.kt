package com.luno.core.data.repository

import com.luno.core.data.datasource.ConversationRemoteDataSource
import com.luno.core.data.datasource.MediaRemoteDataSource
import com.luno.core.data.local.dao.ConversationDao
import com.luno.core.data.local.dao.MessageDao
import com.luno.core.data.mapper.EntityMappers.toDomain
import com.luno.core.data.mapper.EntityMappers.toEntity
import com.luno.core.data.mapper.MessageMapper
import com.luno.core.data.mapper.UserMapper
import com.luno.core.data.model.MessageDto
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.model.Message
import com.luno.core.domain.model.MessageStatus
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.ConversationRepository
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ConversationRepositoryImpl(
    private val remoteDataSource: ConversationRemoteDataSource,
    private val conversationDao: ConversationDao? = null,
    private val messageDao: MessageDao? = null,
    private val mediaRemoteDataSource: MediaRemoteDataSource? = null
) : ConversationRepository {

    private val _conversationsFlow = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversationsFlow: Flow<List<Conversation>> =
        conversationDao?.getConversations()?.map { entities ->
            entities.map { it.toDomain() }
        } ?: _conversationsFlow.asStateFlow()

    private val _messagesFlowMap = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val _typingFlowMap = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val _messageJobs = mutableMapOf<String, Job>()
    private val _messageChannels = mutableMapOf<String, RealtimeChannel>()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var userStatusChannelJob: Job? = null

    init {
        repositoryScope.launch {
            syncPendingMessages()
        }
    }

    override fun getTypingStatus(convId: String): Flow<Boolean> {
        if (!_typingFlowMap.containsKey(convId)) {
            _typingFlowMap[convId] = MutableStateFlow(false)
        }
        return _typingFlowMap[convId]!!.asStateFlow()
    }

    override suspend fun sendTypingStatus(convId: String, isTyping: Boolean) {
        try {
            val currentUserId = remoteDataSource.getCurrentUserId() ?: return
            val channel = _messageChannels[convId] ?: return
            channel.broadcast(
                event = "typing",
                message = JsonObject(
                    mapOf(
                        "user_id" to JsonPrimitive(currentUserId),
                        "is_typing" to JsonPrimitive(isTyping)
                    )
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadOlderMessages(convId: String) {
        try {
            val currentMessages = _messagesFlowMap[convId]?.value
                ?: messageDao?.getMessagesForConversationOnce(convId)?.map { it.toDomain() }
                ?: emptyList()

            val oldestMessage = currentMessages.minByOrNull { it.timestamp } ?: return
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val oldestIso = dateFormat.format(Date(oldestMessage.timestamp))

            val olderDtos = remoteDataSource.fetchOlderMessages(convId, oldestIso, limit = 20)
            if (olderDtos.isEmpty()) return

            val olderMessages = olderDtos.map { MessageMapper.mapToDomain(it) }

            if (messageDao != null) {
                messageDao.upsertMessages(olderMessages.map { it.toEntity(convId) })
            } else {
                val merged =
                    (currentMessages + olderMessages).distinctBy { it.id }.sortedBy { it.timestamp }
                _messagesFlowMap[convId]?.value = merged
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncPendingMessages() {
        val pending = messageDao?.getPendingMessages().orEmpty()
        for (msgEntity in pending) {
            try {
                val currentUserId = msgEntity.senderId
                val msgDto = remoteDataSource.sendMessageRecord(
                    convId = msgEntity.conversationId,
                    senderId = currentUserId,
                    text = msgEntity.content,
                    mediaUrl = msgEntity.mediaUrl,
                    mediaType = msgEntity.mediaType,
                    fileName = msgEntity.fileName,
                    fileSize = msgEntity.fileSize,
                    replyToId = msgEntity.replyToId,
                    replyToText = msgEntity.replyToText,
                    replyToSender = msgEntity.replyToSender
                )
                val sentMsg = MessageMapper.mapToDomain(msgDto)
                messageDao?.deleteMessage(msgEntity.id)
                messageDao?.upsertMessage(sentMsg.toEntity(msgEntity.conversationId))
                remoteDataSource.updateLastMessageId(msgEntity.conversationId, sentMsg.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
                    isRead = true,
                    status = MessageStatus.SENT
                )

                if (!lastMsgId.isNullOrBlank()) {
                    val msgDto = remoteDataSource.fetchMessageRecord(lastMsgId)
                    if (msgDto != null) {
                        lastMessage = MessageMapper.mapToDomain(msgDto)
                    }
                }

                val conv = Conversation(
                    id = convId,
                    recipient = recipient,
                    lastMessage = lastMessage,
                    unreadCount = 0,
                    isPinned = false
                )
                conversationList.add(conv)
            }

            _conversationsFlow.value = conversationList
            conversationDao?.upsertConversations(conversationList.map { it.toEntity() })
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
                        conversationDao?.upsertConversations(currentConvs.map { it.toEntity() })
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
            if (isOnline) {
                syncPendingMessages()
            }
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
                    isRead = true,
                    status = MessageStatus.SENT
                ),
                unreadCount = 0,
                isPinned = false
            )

            val currentList = _conversationsFlow.value.toMutableList()
            if (currentList.none { it.id == newConvId }) {
                currentList.add(0, newConversation)
                _conversationsFlow.value = currentList
                conversationDao?.upsertConversation(newConversation.toEntity())
            }

            Result.success(newConvId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun createGroupConversation(
        title: String,
        memberIds: List<String>
    ): Result<String> {
        return try {
            val currentUserId = remoteDataSource.getCurrentUserId()
                ?: return Result.failure(Exception("Not logged in"))

            val allMembers = (memberIds + currentUserId).distinct()
            val newConvId = remoteDataSource.createGroupConversation(title, allMembers)

            val newConversation = Conversation(
                id = newConvId,
                recipient = User(id = "", name = title.ifBlank { "Nhóm mới" }, avatarUrl = ""),
                lastMessage = Message(
                    id = "m_default",
                    senderId = "",
                    text = "Nhóm đã được tạo",
                    timestamp = System.currentTimeMillis(),
                    isRead = true,
                    status = MessageStatus.SENT
                ),
                unreadCount = 0,
                isPinned = false
            )

            val currentList = _conversationsFlow.value.toMutableList()
            currentList.add(0, newConversation)
            _conversationsFlow.value = currentList
            conversationDao?.upsertConversation(newConversation.toEntity())

            Result.success(newConvId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getConversationDetails(convId: String): Conversation? {
        getConversation(convId)?.let { return it }
        val dbConv = conversationDao?.getConversationById(convId)
        if (dbConv != null) return dbConv.toDomain()

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
                isRead = true,
                status = MessageStatus.SENT
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

            conversationDao?.upsertConversation(conversation.toEntity())
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
                isRead = true,
                status = MessageStatus.SENT
            ),
            unreadCount = 0,
            isPinned = false
        )
    }

    override fun getMessagesForConversation(convId: String): Flow<List<Message>> {
        if (messageDao != null) {
            repositoryScope.launch {
                subscribeToMessages(convId)
                fetchMessages(convId)
            }
            return messageDao.getMessagesForConversation(convId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

        if (!_messagesFlowMap.containsKey(convId)) {
            _messagesFlowMap[convId] = MutableStateFlow(emptyList())
            repositoryScope.launch {
                subscribeToMessages(convId)
            }
        }
        repositoryScope.launch {
            fetchMessages(convId)
        }
        return _messagesFlowMap[convId]!!.asStateFlow()
    }

    override fun stopObservingMessages(convId: String) {
        _messageJobs.remove(convId)?.cancel()
        _messageChannels.remove(convId)?.let { channel ->
            repositoryScope.launch { channel.unsubscribe() }
        }
    }

    private suspend fun fetchMessages(convId: String) {
        try {
            val messageDtos = remoteDataSource.fetchMessages(convId)
            val newMessages = messageDtos.map { MessageMapper.mapToDomain(it) }

            if (messageDao != null) {
                messageDao.upsertMessages(newMessages.map { it.toEntity(convId) })
            } else {
                val current = _messagesFlowMap[convId]?.value.orEmpty()
                val merged = (current + newMessages).distinctBy { it.id }.sortedBy { it.timestamp }
                _messagesFlowMap[convId]?.value = merged
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun subscribeToMessages(convId: String) {
        if (_messageChannels.containsKey(convId)) return
        val channel = remoteDataSource.createMessageChannel(convId)
        _messageChannels[convId] = channel

        channel.broadcastFlow<JsonObject>(event = "typing")
            .onEach { payload ->
                val typingUserId = payload["user_id"]?.jsonPrimitive?.content
                val isTyping = payload["is_typing"]?.jsonPrimitive?.content?.toBoolean() ?: false
                val currentUserId = remoteDataSource.getCurrentUserId()
                if (typingUserId != null && typingUserId != currentUserId) {
                    if (!_typingFlowMap.containsKey(convId)) {
                        _typingFlowMap[convId] = MutableStateFlow(false)
                    }
                    _typingFlowMap[convId]?.value = isTyping
                }
            }.launchIn(repositoryScope)

        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, convId)
        }.onEach { update ->
            val record = update.record
            val id = record["id"]?.jsonPrimitive?.content ?: return@onEach
            val content = record["content"]?.jsonPrimitive?.content ?: "Tin nhắn đã được thu hồi"
            val isUnsent = record["is_unsent"]?.jsonPrimitive?.content?.toBoolean() ?: false

            if (messageDao != null) {
                val existingList = messageDao.getMessagesForConversationOnce(convId)
                val existing = existingList.find { it.id == id }
                if (existing != null) {
                    val updatedEntity = existing.copy(
                        content = content,
                        isUnsent = isUnsent,
                        mediaUrl = null,
                        fileName = null,
                        fileSize = null
                    )
                    messageDao.upsertMessage(updatedEntity)
                }
            } else {
                val current = _messagesFlowMap[convId]?.value.orEmpty().toMutableList()
                val index = current.indexOfFirst { it.id == id }
                if (index != -1) {
                    current[index] = current[index].copy(
                        text = content,
                        isUnsent = isUnsent,
                        mediaUrl = null,
                        fileName = null,
                        fileSize = null
                    )
                    _messagesFlowMap[convId]?.value = current
                }
            }
        }.launchIn(repositoryScope)

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
                val mediaUrl = record["media_url"]?.jsonPrimitive?.content
                val mediaTypeStr = record["media_type"]?.jsonPrimitive?.content ?: "TEXT"
                val fileName = record["file_name"]?.jsonPrimitive?.content
                val fileSize = record["file_size"]?.jsonPrimitive?.content
                val replyToId = record["reply_to_id"]?.jsonPrimitive?.content
                val replyToText = record["reply_to_text"]?.jsonPrimitive?.content
                val replyToSender = record["reply_to_sender"]?.jsonPrimitive?.content
                val isUnsent = record["is_unsent"]?.jsonPrimitive?.content?.toBoolean() ?: false

                val messageDto = MessageDto(
                    id = id,
                    conversationId = convId,
                    senderId = senderId,
                    content = content,
                    createdAt = createdAt,
                    mediaUrl = mediaUrl,
                    mediaType = mediaTypeStr,
                    fileName = fileName,
                    fileSize = fileSize,
                    replyToId = replyToId,
                    replyToText = replyToText,
                    replyToSender = replyToSender,
                    isUnsent = isUnsent
                )
                val message = MessageMapper.mapToDomain(messageDto)

                if (messageDao != null) {
                    messageDao.upsertMessage(message.toEntity(convId))
                } else {
                    val current = _messagesFlowMap[convId]?.value.orEmpty()
                    if (current.none { it.id == id }) {
                        _messagesFlowMap[convId]?.value =
                            (current + message).sortedBy { it.timestamp }
                    }
                }

                val currentConvs = _conversationsFlow.value.toMutableList()
                val index = currentConvs.indexOfFirst { it.id == convId }
                if (index != -1) {
                    val updatedConv = currentConvs[index].copy(lastMessage = message)
                    currentConvs[index] = updatedConv
                    _conversationsFlow.value = currentConvs
                    conversationDao?.upsertConversation(updatedConv.toEntity())
                }
            }.launchIn(repositoryScope)

        channel.subscribe()
    }

    override suspend fun sendMessage(convId: String, text: String): Result<Unit> {
        return sendMediaMessage(
            convId = convId,
            text = text,
            mediaUrl = "",
            mediaType = MediaType.TEXT
        )
    }

    override suspend fun sendMediaMessage(
        convId: String,
        text: String,
        mediaUrl: String,
        mediaType: MediaType,
        fileName: String?,
        fileSize: String?,
        replyToId: String?,
        replyToText: String?,
        replyToSender: String?
    ): Result<Unit> {
        val currentUserId = remoteDataSource.getCurrentUserId() ?: "me"
        val tempMsgId = "pending_${UUID.randomUUID()}"
        val localMessage = Message(
            id = tempMsgId,
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = true,
            status = MessageStatus.PENDING,
            mediaUrl = mediaUrl.ifBlank { null },
            mediaType = mediaType,
            fileName = fileName,
            fileSize = fileSize,
            replyToMessageId = replyToId,
            replyToText = replyToText,
            replyToSenderName = replyToSender
        )

        if (messageDao != null) {
            messageDao.upsertMessage(localMessage.toEntity(convId))
        } else {
            val currentList = _messagesFlowMap[convId]?.value.orEmpty().toMutableList()
            currentList.add(localMessage)
            _messagesFlowMap[convId]?.value = currentList
        }

        return try {
            val msgDto = remoteDataSource.sendMessageRecord(
                convId = convId,
                senderId = currentUserId,
                text = text,
                mediaUrl = mediaUrl,
                mediaType = mediaType.name,
                fileName = fileName,
                fileSize = fileSize,
                replyToId = replyToId,
                replyToText = replyToText,
                replyToSender = replyToSender
            )
            val sentMessage = MessageMapper.mapToDomain(msgDto)

            if (messageDao != null) {
                messageDao.deleteMessage(tempMsgId)
                messageDao.upsertMessage(sentMessage.toEntity(convId))
            } else {
                val list = _messagesFlowMap[convId]?.value.orEmpty().toMutableList()
                list.removeAll { it.id == tempMsgId || it.id == sentMessage.id }
                list.add(sentMessage)
                _messagesFlowMap[convId]?.value = list.sortedBy { it.timestamp }
            }

            val currentConvs = _conversationsFlow.value.toMutableList()
            val index = currentConvs.indexOfFirst { it.id == convId }
            if (index != -1) {
                val conv = currentConvs[index]
                val updatedConv = conv.copy(lastMessage = sentMessage, unreadCount = 0)
                currentConvs[index] = updatedConv
                _conversationsFlow.value = currentConvs
                conversationDao?.upsertConversation(updatedConv.toEntity())
            }

            remoteDataSource.updateLastMessageId(convId, sentMessage.id)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun unsendMessage(convId: String, messageId: String): Result<Unit> {
        val targetMessage = _messagesFlowMap[convId]?.value?.find { it.id == messageId }
            ?: messageDao?.getMessagesForConversationOnce(convId)?.map { it.toDomain() }
                ?.find { it.id == messageId }

        if (targetMessage != null) {
            val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - targetMessage.timestamp > ONE_DAY_MILLIS) {
                return Result.failure(Exception("Không thể thu hồi tin nhắn đã gửi quá 24 giờ"))
            }

            val mediaUrl = targetMessage.mediaUrl
            if (!mediaUrl.isNullOrBlank()) {
                val bucketName = when (targetMessage.mediaType) {
                    MediaType.IMAGE -> "chat-images"
                    MediaType.AUDIO -> "chat-audio"
                    MediaType.FILE -> "chat-files"
                    else -> "chat-media"
                }
                repositoryScope.launch {
                    try {
                        mediaRemoteDataSource?.deleteFile(mediaUrl, bucketName)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        return try {
            remoteDataSource.unsendMessageRecord(messageId)

            val unsentText = "Tin nhắn đã được thu hồi"
            if (messageDao != null) {
                val existingList = messageDao.getMessagesForConversationOnce(convId)
                val existing = existingList.find { it.id == messageId }
                if (existing != null) {
                    val updatedEntity = existing.copy(
                        content = unsentText,
                        isUnsent = true,
                        mediaUrl = null,
                        fileName = null,
                        fileSize = null
                    )
                    messageDao.upsertMessage(updatedEntity)
                }
            }

            val currentMessages = _messagesFlowMap[convId]?.value.orEmpty().map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        text = unsentText,
                        isUnsent = true,
                        mediaUrl = null,
                        fileName = null,
                        fileSize = null
                    )
                } else msg
            }
            _messagesFlowMap[convId]?.value = currentMessages

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
