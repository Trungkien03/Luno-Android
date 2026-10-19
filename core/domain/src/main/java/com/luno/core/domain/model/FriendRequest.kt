package com.luno.core.domain.model

enum class FriendRequestStatus {
    PENDING, ACCEPTED, REJECTED
}

data class FriendRequest(
    val id: String,
    val sender: User,
    val receiver: User,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
