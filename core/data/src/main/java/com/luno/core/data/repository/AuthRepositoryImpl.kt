package com.luno.core.data.repository

import com.luno.core.domain.repository.AuthRepository
import com.luno.core.network.di.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken

class AuthRepositoryImpl : AuthRepository {
    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            SupabaseModule.client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}