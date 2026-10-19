package com.luno.feature.chat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luno.core.domain.model.Comment
import com.luno.core.domain.model.Post
import com.luno.core.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val postRepository: PostRepository
) : ViewModel() {

    val feedPosts: StateFlow<List<Post>> = postRepository.getFeedPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCreatingPost = MutableStateFlow(false)
    val isCreatingPost: StateFlow<Boolean> = _isCreatingPost.asStateFlow()

    private val _selectedPostForComments = MutableStateFlow<Post?>(null)
    val selectedPostForComments: StateFlow<Post?> = _selectedPostForComments.asStateFlow()

    private val _commentsForSelectedPost = MutableStateFlow<List<Comment>>(emptyList())
    val commentsForSelectedPost: StateFlow<List<Comment>> = _commentsForSelectedPost.asStateFlow()

    init {
        refreshFeed()
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                postRepository.fetchFeedPosts()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPost(content: String, images: List<Pair<ByteArray, String>>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isCreatingPost.value = true
            val result = postRepository.createPost(content, images)
            _isCreatingPost.value = false
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            postRepository.toggleLikePost(postId)
        }
    }

    fun openCommentsForPost(post: Post) {
        _selectedPostForComments.value = post
        viewModelScope.launch {
            postRepository.fetchCommentsForPost(post.id)
            postRepository.getCommentsForPost(post.id).collect { comments ->
                _commentsForSelectedPost.value = comments
            }
        }
    }

    fun closeComments() {
        _selectedPostForComments.value = null
        _commentsForSelectedPost.value = emptyList()
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            postRepository.addComment(postId, text)
        }
    }
}
