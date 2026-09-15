package com.luno.core.domain.repository

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
}