package com.luno.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luno.core.data.ChatRepository
import com.luno.core.model.Conversation
import com.luno.core.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    val conversations: StateFlow<List<Conversation>> = repository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages.asStateFlow()

    fun selectConversation(convId: String?) {
        _selectedConversationId.value = convId
        if (convId != null) {
            viewModelScope.launch {
                repository.getMessagesForConversation(convId).collect { msgs ->
                    _currentMessages.value = msgs
                }
            }
        } else {
            _currentMessages.value = emptyList()
        }
    }

    fun sendMessage(text: String) {
        val convId = _selectedConversationId.value ?: return
        repository.sendMessage(convId, text)
    }

    fun getConversation(convId: String): Conversation? {
        return repository.getConversation(convId)
    }
}
