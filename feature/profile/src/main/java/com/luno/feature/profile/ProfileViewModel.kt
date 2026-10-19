package com.luno.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.repository.AuthRepository
import com.luno.core.domain.repository.MediaRepository
import com.luno.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository? = null,
    private val mediaRepository: MediaRepository? = null
) : ViewModel() {
    val email: StateFlow<String?> = authRepository.userEmailFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUserEmail()
        )

    val userImg: StateFlow<String?> = authRepository.userImgFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUserImg()
        )

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    private val _profileSuccessMessage = MutableStateFlow<String?>(null)
    val profileSuccessMessage: StateFlow<String?> = _profileSuccessMessage.asStateFlow()

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarBytes: ByteArray?,
        avatarFileName: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSavingProfile.value = true
            try {
                var uploadedAvatarUrl: String? = null
                if (avatarBytes != null && avatarFileName != null && mediaRepository != null) {
                    val uploadResult = mediaRepository.uploadMedia(
                        bytes = avatarBytes,
                        fileName = avatarFileName,
                        mediaType = MediaType.IMAGE
                    )
                    uploadedAvatarUrl = uploadResult.getOrNull()
                }

                if (userRepository != null) {
                    userRepository.updateUserProfile(
                        displayName = displayName,
                        username = username,
                        bio = bio,
                        avatarUrl = uploadedAvatarUrl,
                        gender = null,
                        dob = null
                    )
                }
                authRepository.refreshUserInfo()
                _profileSuccessMessage.value = "Cập nhật hồ sơ thành công"
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSavingProfile.value = false
            }
        }
    }

    fun clearSuccessMessage() {
        _profileSuccessMessage.value = null
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            _isSigningOut.value = true
            authRepository.signOut()
            _isSigningOut.value = false
            onSignedOut()
        }
    }
}
