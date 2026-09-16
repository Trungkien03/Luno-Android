package com.luno.core.auth.ui

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.luno.core.auth.BuildConfig
import com.luno.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Trạng thái của phiên đăng nhập
sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val userId: String, val email: String) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoadingAuth = MutableStateFlow(true)
    val isLoadingAuth: StateFlow<Boolean> = _isLoadingAuth.asStateFlow()

    val userEmail: StateFlow<String?> = authRepository.userEmailFlow
        .onEach { _isLoadingAuth.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUserEmail()
        )

    init {
        viewModelScope.launch {
            try {
                authRepository.refreshUserInfo()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to refresh user info on startup", e)
            }
        }
    }

    private val webClientId = BuildConfig.GOOGLE_CLIENT_ID

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if ((credential is CustomCredential) && (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id

                    val result = authRepository.signInWithGoogle(idToken)

                    if (result.isSuccess) {
                        Log.d("AuthViewModel", "Login Success! Email: $email")
                        _authState.value = AuthState.Success(userId = email, email = email)
                    } else {
                        val exception = result.exceptionOrNull()
                        Log.e("AuthViewModel", "Supabase auth failed", exception)
                        _authState.value =
                            AuthState.Error(exception?.message ?: "Lỗi xác thực Supabase")
                    }
                } else {
                    Log.e("AuthViewModel", "Unexpected type of credential")
                    _authState.value = AuthState.Error("Loại chứng thực không mong muốn")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in failed", e)

                // MOCK ĐỂ TEST UI LUÔN BỎ QUA LỖI BẢO MẬT/CLIENT_ID LÚC CHƯA CÓ CONFIG
                // NOTE: Bỏ đoạn mock này khi đã config Firebase/Google Cloud Console
                // _authState.value = AuthState.Success(userId = "mock-user-1", email = "test@gmail.com")
                _authState.value = AuthState.Error(e.localizedMessage ?: "Đăng nhập thất bại")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
