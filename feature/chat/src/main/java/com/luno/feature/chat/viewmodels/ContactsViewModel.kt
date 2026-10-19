package com.luno.feature.chat.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kane.luno.utils.ContactsHelper
import com.kane.luno.utils.DeviceContact
import com.luno.core.domain.model.FriendRequest
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.ConversationRepository
import com.luno.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    val friendsList: StateFlow<List<User>> = userRepository.getFriendsList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<FriendRequest>> = userRepository.getPendingFriendRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun syncDeviceContacts(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val contacts = ContactsHelper.getDeviceContacts(context)
                _deviceContacts.value = contacts
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Không thể đọc danh bạ máy: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            userRepository.acceptFriendRequest(requestId)
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            userRepository.rejectFriendRequest(requestId)
        }
    }

    fun startConversation(userId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = conversationRepository.startConversation(userId)
            result.onSuccess(onSuccess).onFailure { error ->
                _errorMessage.value = error.localizedMessage
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
