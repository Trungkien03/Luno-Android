package com.luno.core.data.repository

import com.luno.core.data.datasource.MediaRemoteDataSource
import com.luno.core.data.mapper.UserMapper
import com.luno.core.data.model.CommentDto
import com.luno.core.data.model.PostDto
import com.luno.core.data.model.UserDto
import com.luno.core.domain.model.Comment
import com.luno.core.domain.model.Post
import com.luno.core.domain.model.User
import com.luno.core.domain.repository.PostRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class PostRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val mediaRemoteDataSource: MediaRemoteDataSource = MediaRemoteDataSource(supabaseClient)
) : PostRepository {

    private val _feedPostsFlow = MutableStateFlow<List<Post>>(emptyList())
    private val _commentsFlowMap = mutableMapOf<String, MutableStateFlow<List<Comment>>>()

    override fun getFeedPosts(): Flow<List<Post>> {
        return _feedPostsFlow.asStateFlow()
    }

    override suspend fun fetchFeedPosts(): Result<Unit> {
        return try {
            val postDtos = supabaseClient.postgrest["posts"]
                .select()
                .decodeList<PostDto>()

            val posts = mutableListOf<Post>()
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id.orEmpty()

            for (dto in postDtos) {
                val userDto = supabaseClient.postgrest["users"]
                    .select { filter { eq("id", dto.userId) } }
                    .decodeSingleOrNull<UserDto>()

                val author = userDto?.let { UserMapper.mapToDomain(it) }
                    ?: User(id = dto.userId, name = "Người dùng", avatarUrl = "")

                val isLiked = try {
                    val likes = supabaseClient.postgrest["post_likes"]
                        .select {
                            filter {
                                eq("post_id", dto.id)
                                eq("user_id", currentUserId)
                            }
                        }.decodeList<JsonObject>()
                    likes.isNotEmpty()
                } catch (_: Exception) {
                    false
                }

                val timestamp = parseUtcEpoch(dto.createdAt)
                posts.add(
                    Post(
                        id = dto.id,
                        author = author,
                        content = dto.content,
                        mediaUrls = dto.mediaUrls,
                        timestamp = timestamp,
                        likeCount = dto.likeCount,
                        commentCount = dto.commentCount,
                        isLiked = isLiked
                    )
                )
            }

            _feedPostsFlow.value = posts.sortedByDescending { it.timestamp }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun createPost(
        content: String,
        imageBytesList: List<Pair<ByteArray, String>>
    ): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            val userDto = supabaseClient.postgrest["users"]
                .select { filter { eq("id", currentUserId) } }
                .decodeSingleOrNull<UserDto>()

            val currentUser = userDto?.let { UserMapper.mapToDomain(it) }
                ?: User(id = currentUserId, name = "Bạn", avatarUrl = "")

            val uploadedUrls = mutableListOf<String>()
            for ((bytes, fileName) in imageBytesList) {
                val url = mediaRemoteDataSource.uploadFile(bytes, fileName, "chat-images")
                if (url.isNotBlank()) {
                    uploadedUrls.add(url)
                }
            }

            val optimisticPost = Post(
                id = UUID.randomUUID().toString(),
                author = currentUser,
                content = content,
                mediaUrls = uploadedUrls,
                timestamp = System.currentTimeMillis(),
                likeCount = 0,
                commentCount = 0,
                isLiked = false
            )

            // Optimistically update UI feed immediately
            _feedPostsFlow.value = listOf(optimisticPost) + _feedPostsFlow.value

            val mediaJsonArray = JsonArray(uploadedUrls.map { JsonPrimitive(it) })
            supabaseClient.postgrest["posts"].insert(
                JsonObject(
                    mapOf(
                        "user_id" to JsonPrimitive(currentUserId),
                        "content" to JsonPrimitive(content),
                        "media_urls" to mediaJsonArray
                    )
                )
            )

            fetchFeedPosts()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun toggleLikePost(postId: String): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            val currentPosts = _feedPostsFlow.value.toMutableList()
            val index = currentPosts.indexOfFirst { it.id == postId }
            if (index != -1) {
                val post = currentPosts[index]
                val newIsLiked = !post.isLiked
                val newLikeCount =
                    if (newIsLiked) post.likeCount + 1 else (post.likeCount - 1).coerceAtLeast(0)

                currentPosts[index] = post.copy(isLiked = newIsLiked, likeCount = newLikeCount)
                _feedPostsFlow.value = currentPosts

                if (newIsLiked) {
                    supabaseClient.postgrest["post_likes"].insert(
                        JsonObject(
                            mapOf(
                                "post_id" to JsonPrimitive(postId),
                                "user_id" to JsonPrimitive(currentUserId)
                            )
                        )
                    )
                } else {
                    supabaseClient.postgrest["post_likes"].delete {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", currentUserId)
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun getCommentsForPost(postId: String): Flow<List<Comment>> {
        if (!_commentsFlowMap.containsKey(postId)) {
            _commentsFlowMap[postId] = MutableStateFlow(emptyList())
        }
        return _commentsFlowMap[postId]!!.asStateFlow()
    }

    override suspend fun fetchCommentsForPost(postId: String): Result<Unit> {
        return try {
            val commentDtos = supabaseClient.postgrest["post_comments"]
                .select {
                    filter { eq("post_id", postId) }
                }.decodeList<CommentDto>()

            val comments = mutableListOf<Comment>()
            for (dto in commentDtos) {
                val userDto = supabaseClient.postgrest["users"]
                    .select { filter { eq("id", dto.userId) } }
                    .decodeSingleOrNull<UserDto>()

                val author = userDto?.let { UserMapper.mapToDomain(it) }
                    ?: User(id = dto.userId, name = "Người dùng", avatarUrl = "")

                comments.add(
                    Comment(
                        id = dto.id,
                        postId = postId,
                        author = author,
                        content = dto.content,
                        timestamp = parseUtcEpoch(dto.createdAt)
                    )
                )
            }

            if (!_commentsFlowMap.containsKey(postId)) {
                _commentsFlowMap[postId] = MutableStateFlow(emptyList())
            }
            _commentsFlowMap[postId]?.value = comments.sortedBy { it.timestamp }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun addComment(postId: String, content: String): Result<Unit> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            supabaseClient.postgrest["post_comments"].insert(
                JsonObject(
                    mapOf(
                        "post_id" to JsonPrimitive(postId),
                        "user_id" to JsonPrimitive(currentUserId),
                        "content" to JsonPrimitive(content)
                    )
                )
            )

            fetchCommentsForPost(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun parseUtcEpoch(createdAt: String?): Long {
        if (createdAt.isNullOrBlank()) return System.currentTimeMillis()

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(createdAt)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }
}
