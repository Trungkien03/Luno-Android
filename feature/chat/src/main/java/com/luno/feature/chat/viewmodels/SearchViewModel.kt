package com.luno.feature.chat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.ConversationRepository
import com.luno.core.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchedUsers = MutableStateFlow<List<User>>(emptyList())
    val searchedUsers: StateFlow<List<User>> = _searchedUsers.asStateFlow()

    private val conversationsFlow: StateFlow<List<Conversation>> =
        conversationRepository.conversationsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredConversations: StateFlow<List<Conversation>> = combine(
        conversationsFlow,
        _query
    ) { conversations, searchQuery ->
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            conversations.filter {
                it.recipient.name.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.text.contains(searchQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _searchedUsers.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.searchUsers(newQuery).onSuccess { users ->
                    val currentUserId = conversationRepository.getCurrentUserId()
                    _searchedUsers.value = users.filter { it.id != currentUserId }
                }.onFailure { error ->
                    error.printStackTrace()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startConversation(recipientId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = conversationRepository.startConversation(recipientId)
            _isLoading.value = false

            result.onSuccess { convId ->
                onSuccess(convId)
            }.onFailure { error ->
                error.printStackTrace()
                _errorMessage.value =
                    "Không thể tạo cuộc trò chuyện: ${error.localizedMessage ?: error.toString()}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
