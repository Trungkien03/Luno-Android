package com.luno.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signInWithPhone(phone: String): Result<Unit>
    suspend fun verifyPhoneOtp(phone: String, token: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshUserInfo(): Result<Unit>
    fun currentUserEmail(): String?
    fun currentUserImg(): String?
    val userEmailFlow: Flow<String?>
    val userImgFlow: Flow<String?>
}
