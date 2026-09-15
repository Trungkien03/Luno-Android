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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Trạng thái của phiên đăng nhập
sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val userId: String, val email: String) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Bạn cần thay thế chuỗi này bằng chuỗi lấy từ google-services.json hoặc file config
    // Ví dụ: BuildConfig.WEB_CLIENT_ID
    private val webClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

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
                    // Thành công: Lấy ID Token và gửi lên backend để verify
                    @Suppress("unused")
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id

                    Log.d("AuthViewModel", "Login Success! Email: $email")
                    _authState.value = AuthState.Success(userId = email, email = email)
                } else {
                    Log.e("AuthViewModel", "Unexpected type of credential")
                    _authState.value = AuthState.Error("Loại chứng thực không mong muốn")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in failed", e)

                // MOCK ĐỂ TEST UI LUÔN BỎ QUA LỖI BẢO MẬT/CLIENT_ID LÚC CHƯA CÓ CONFIG
                // NOTE: Bỏ đoạn mock này khi đã config Firebase/Google Cloud Console
                _authState.value =
                    AuthState.Success(userId = "mock-user-1", email = "test@gmail.com")
                // _authState.value = AuthState.Error(e.localizedMessage ?: "Đăng nhập thất bại")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}