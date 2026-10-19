package com.luno.core.domain.repository

import com.luno.core.domain.model.Comment
import com.luno.core.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getFeedPosts(): Flow<List<Post>>
    suspend fun fetchFeedPosts(): Result<Unit>
    suspend fun createPost(
        content: String,
        imageBytesList: List<Pair<ByteArray, String>> = emptyList()
    ): Result<Unit>

    suspend fun toggleLikePost(postId: String): Result<Unit>
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    suspend fun fetchCommentsForPost(postId: String): Result<Unit>
    suspend fun addComment(postId: String, content: String): Result<Unit>
}
