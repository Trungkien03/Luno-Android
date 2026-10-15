package com.luno.feature.chat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.model.Message
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.ConversationRepository
import com.luno.core.domain.repository.MediaRepository
import com.luno.core.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository? = null
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = conversationRepository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: String?
        get() = conversationRepository.getCurrentUserId()

    private val _searchedUsers = MutableStateFlow<List<User>>(emptyList())
    val searchedUsers: StateFlow<List<User>> = _searchedUsers.asStateFlow()

    private val _isAddDialogVisible = MutableStateFlow(false)
    val isAddDialogVisible: StateFlow<Boolean> = _isAddDialogVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _replyingMessage = MutableStateFlow<Message?>(null)
    val replyingMessage: StateFlow<Message?> = _replyingMessage.asStateFlow()

    init {
        refreshConversations()
    }

    fun refreshConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                conversationRepository.updateOnlineStatus(true)
                conversationRepository.fetchConversations()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setAddDialogVisible(visible: Boolean) {
        _isAddDialogVisible.value = visible
        if (visible) {
            searchUsers("")
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            userRepository.searchUsers(query).onSuccess { users ->
                _searchedUsers.value = users
            }.onFailure { error ->
                error.printStackTrace()
            }
        }
    }

    fun startConversation(recipientId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = conversationRepository.startConversation(recipientId)
            result.onSuccess { convId ->
                _isAddDialogVisible.value = false
                onSuccess(convId)
            }.onFailure { error ->
                error.printStackTrace()
                _errorMessage.value =
                    "Không thể tạo cuộc trò chuyện: ${error.localizedMessage ?: error.toString()}"
            }
        }
    }

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    val currentConversation: StateFlow<Conversation?> = combine(
        conversations,
        _selectedConversationId
    ) { convList, selectedId ->
        if (selectedId == null) null
        else convList.find { it.id == selectedId } ?: conversationRepository.getConversationDetails(
            selectedId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages.asStateFlow()

    private var messagesJob: Job? = null

    fun selectConversation(convId: String?) {
        val previousId = _selectedConversationId.value
        _selectedConversationId.value = convId
        _replyingMessage.value = null
        messagesJob?.cancel()
        if (previousId != null && previousId != convId) {
            conversationRepository.stopObservingMessages(previousId)
        }

        if (convId != null) {
            messagesJob = viewModelScope.launch {
                conversationRepository.getConversationDetails(convId)

                conversationRepository.getMessagesForConversation(convId).collect { msgs ->
                    _currentMessages.value = msgs
                }
            }
        } else {
            _currentMessages.value = emptyList()
        }
    }

    fun setReplyingMessage(message: Message?) {
        _replyingMessage.value = message
    }

    override fun onCleared() {
        super.onCleared()
        _selectedConversationId.value?.let { conversationRepository.stopObservingMessages(it) }
    }

    fun sendMessage(text: String) {
        val convId = _selectedConversationId.value ?: return
        val reply = _replyingMessage.value
        viewModelScope.launch {
            if (reply != null) {
                val recipientName =
                    if (reply.senderId == currentUserId) "Bạn" else currentConversation.value?.recipient?.name
                        ?: "Người dùng"
                conversationRepository.sendMediaMessage(
                    convId = convId,
                    text = text,
                    mediaUrl = "",
                    mediaType = MediaType.TEXT,
                    replyToId = reply.id,
                    replyToText = reply.text.ifBlank {
                        when (reply.mediaType) {
                            MediaType.IMAGE -> "[Hình ảnh]"
                            MediaType.AUDIO -> "[Ghi âm]"
                            MediaType.FILE -> "[Tệp đính kèm]"
                            else -> reply.text
                        }
                    },
                    replyToSender = recipientName
                )
                _replyingMessage.value = null
            } else {
                conversationRepository.sendMessage(convId, text)
            }
        }
    }

    fun unsendMessage(message: Message) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            conversationRepository.unsendMessage(convId, message.id)
        }
    }

    fun sendMediaMessage(
        bytes: ByteArray,
        fileName: String,
        mediaType: MediaType,
        text: String = ""
    ) {
        val convId = _selectedConversationId.value ?: return
        val reply = _replyingMessage.value
        viewModelScope.launch {
            _isLoading.value = true
            val uploadResult = mediaRepository?.uploadMedia(bytes, fileName, mediaType)
            val mediaUrl = uploadResult?.getOrNull().orEmpty()
            val sizeFormatted = "${bytes.size / 1024} KB"

            val recipientName =
                if (reply?.senderId == currentUserId) "Bạn" else currentConversation.value?.recipient?.name
                    ?: "Người dùng"

            conversationRepository.sendMediaMessage(
                convId = convId,
                text = text.ifBlank {
                    when (mediaType) {
                        MediaType.IMAGE -> "[Hình ảnh]"
                        MediaType.AUDIO -> "[Ghi âm]"
                        MediaType.FILE -> "[Tệp đính kèm]"
                        else -> ""
                    }
                },
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                fileName = fileName,
                fileSize = sizeFormatted,
                replyToId = reply?.id,
                replyToText = reply?.text,
                replyToSender = recipientName
            )
            _replyingMessage.value = null
            _isLoading.value = false
        }
    }

    fun getConversation(convId: String): Conversation? {
        return conversationRepository.getConversation(convId)
    }
}
