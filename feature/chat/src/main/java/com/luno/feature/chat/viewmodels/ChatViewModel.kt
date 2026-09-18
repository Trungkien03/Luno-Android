package com.luno.feature.chat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luno.core.domain.repository.ConversationRepository
import com.luno.core.domain.repository.UserRepository
import com.luno.core.model.Conversation
import com.luno.core.model.Message
import com.luno.core.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = conversationRepository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: String?
        get() = conversationRepository.getCurrentUserId()

    private val _searchedUsers = MutableStateFlow<List<UserDto>>(emptyList())
    val searchedUsers: StateFlow<List<UserDto>> = _searchedUsers.asStateFlow()

    private val _isAddDialogVisible = MutableStateFlow(false)
    val isAddDialogVisible: StateFlow<Boolean> = _isAddDialogVisible.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refreshConversations()
    }

    fun refreshConversations() {
        viewModelScope.launch {
            conversationRepository.fetchConversations()
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

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages.asStateFlow()

    fun selectConversation(convId: String?) {
        _selectedConversationId.value = convId
        if (convId != null) {
            viewModelScope.launch {
                val conv = conversationRepository.getConversationDetails(convId)
                _currentConversation.value = conv

                conversationRepository.getMessagesForConversation(convId).collect { msgs ->
                    _currentMessages.value = msgs
                }
            }
        } else {
            _currentConversation.value = null
            _currentMessages.value = emptyList()
        }
    }

    fun sendMessage(text: String) {
        val convId = _selectedConversationId.value ?: return
        conversationRepository.sendMessage(convId, text)
    }

    fun getConversation(convId: String): Conversation? {
        return conversationRepository.getConversation(convId)
    }
}
