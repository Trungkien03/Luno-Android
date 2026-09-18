package com.luno.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val name: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
) {
    val effectiveName: String
        get() = displayName ?: name ?: email ?: "Người dùng"
}
