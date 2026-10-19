package com.luno.core.domain.repository

import com.luno.core.domain.model.FriendRequest
import com.luno.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun findUserById(userId: String): Result<User>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun sendFriendRequest(receiverId: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>
    fun getFriendsList(): Flow<List<User>>
    fun getPendingFriendRequests(): Flow<List<FriendRequest>>
}
