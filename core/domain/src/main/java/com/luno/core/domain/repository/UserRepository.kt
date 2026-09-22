package com.luno.core.domain.repository

import com.luno.core.domain.model.User

interface UserRepository {
    suspend fun findUserById(userId: String): Result<User?>
    suspend fun searchUsers(query: String): Result<List<User>>
}
