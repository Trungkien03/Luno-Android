package com.luno.core.domain.repository

import com.luno.core.model.UserDto

interface UserRepository {
    suspend fun findUserById(userId: String): Result<UserDto?>
    suspend fun searchUsers(query: String): Result<List<UserDto>>
}
