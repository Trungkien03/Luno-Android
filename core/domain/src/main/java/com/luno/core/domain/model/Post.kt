package com.luno.core.domain.model

data class Post(
    val id: String,
    val author: User,
    val content: String,
    val mediaUrls: List<String> = emptyList(),
    val timestamp: Long,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false
)

data class Comment(
    val id: String,
    val postId: String,
    val author: User,
    val content: String,
    val timestamp: Long
)
