package com.luno.core.data.repository

import com.luno.core.data.mapper.UserMapper
import com.luno.core.data.model.UserDto
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class UserRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : UserRepository {

    override suspend fun findUserById(userId: String): Result<User?> {
        return try {
            val userDto = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserDto>()
            val user = userDto?.let { UserMapper.mapToDomain(it) }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            val userDtos = supabaseClient.postgrest["users"]
                .select()
                .decodeList<UserDto>()
            val filtered = userDtos.filter { user ->
                user.id != currentUserId &&
                        (query.isBlank() ||
                                (user.email?.contains(query, ignoreCase = true) == true) ||
                                (user.name?.contains(query, ignoreCase = true) == true) ||
                                (user.id.contains(query, ignoreCase = true)))
            }
            Result.success(filtered.map { UserMapper.mapToDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
