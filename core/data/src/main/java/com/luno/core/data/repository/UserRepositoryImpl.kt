package com.luno.core.data.repository

import com.luno.core.data.mapper.UserMapper
import com.luno.core.data.model.UserDto
import com.luno.core.domain.model.FriendRequest
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class UserRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : UserRepository {

    private val _friendsFlow = MutableStateFlow<List<User>>(emptyList())
    private val _pendingRequestsFlow = MutableStateFlow<List<FriendRequest>>(emptyList())

    override suspend fun findUserById(userId: String): Result<User> {
        return try {
            val userDto = supabaseClient.postgrest["users"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserDto>()
            val user = userDto?.let { UserMapper.mapToDomain(it) }
                ?: User(id = userId, name = "Người dùng", avatarUrl = "")
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

    override suspend fun sendFriendRequest(receiverId: String): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            supabaseClient.postgrest["friend_requests"].insert(
                JsonObject(
                    mapOf(
                        "sender_id" to JsonPrimitive(currentUserId),
                        "receiver_id" to JsonPrimitive(receiverId),
                        "status" to JsonPrimitive("PENDING")
                    )
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest["friend_requests"].update(
                JsonObject(mapOf("status" to JsonPrimitive("ACCEPTED")))
            ) {
                filter { eq("id", requestId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest["friend_requests"].update(
                JsonObject(mapOf("status" to JsonPrimitive("REJECTED")))
            ) {
                filter { eq("id", requestId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun getFriendsList(): Flow<List<User>> {
        return _friendsFlow.asStateFlow()
    }

    override fun getPendingFriendRequests(): Flow<List<FriendRequest>> {
        return _pendingRequestsFlow.asStateFlow()
    }

    override suspend fun updateUserProfile(
        displayName: String,
        username: String?,
        bio: String?,
        avatarUrl: String?,
        gender: String?,
        dob: String?
    ): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            val updateMap = mutableMapOf<String, JsonPrimitive>(
                "display_name" to JsonPrimitive(displayName)
            )
            if (!username.isNullOrBlank()) updateMap["username"] = JsonPrimitive(username.trim())
            if (bio != null) updateMap["bio"] = JsonPrimitive(bio)
            if (!avatarUrl.isNullOrBlank()) updateMap["avatar_url"] = JsonPrimitive(avatarUrl)
            if (!gender.isNullOrBlank()) updateMap["gender"] = JsonPrimitive(gender)
            if (!dob.isNullOrBlank()) updateMap["dob"] = JsonPrimitive(dob)

            supabaseClient.postgrest["users"].update(JsonObject(updateMap)) {
                filter { eq("id", currentUserId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
