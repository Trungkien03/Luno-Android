package com.luno.core.data.mapper

import com.luno.core.data.model.UserDto
import com.luno.core.domain.model.User

object UserMapper {
    fun mapToDomain(dto: UserDto): User {
        return User(
            id = dto.id,
            name = dto.effectiveName,
            avatarUrl = dto.avatarUrl.orEmpty(),
            isOnline = dto.isOnline ?: false,
            lastSeenAt = dto.lastSeenAt
        )
    }
}
