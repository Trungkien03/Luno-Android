package com.luno.core.data.model

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
    val avatarUrl: String? = null,
    @SerialName("is_online")
    val isOnline: Boolean? = null,
    @SerialName("last_seen_at")
    val lastSeenAt: String? = null
) {
    val effectiveName: String
        get() = displayName ?: name ?: email ?: "Người dùng"
}
