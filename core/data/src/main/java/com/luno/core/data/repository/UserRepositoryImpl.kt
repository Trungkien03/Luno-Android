package com.luno.core.data.repository

import com.luno.core.domain.repository.UserRepository
import com.luno.core.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class UserRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : UserRepository {

    override suspend fun findUserById(userId: String): Result<UserDto?> {
        return try {
            val user = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserDto>()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}