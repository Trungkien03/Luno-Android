package com.luno.feature.chat.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.Message
import com.luno.core.domain.model.User
import com.luno.feature.chat.ui.ChatDetailScreen
import com.luno.feature.chat.ui.ChatListScreen
import com.luno.feature.chat.ui.SearchScreen
import com.luno.feature.chat.viewmodels.ChatViewModel
import com.luno.feature.chat.viewmodels.SearchViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ChatsRoute

@Serializable
data object SearchRoute

@Serializable
data class ChatDetailRoute(val conversationId: String)

fun NavGraphBuilder.chatGraph(
    viewModel: ChatViewModel,
    searchViewModel: SearchViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onBack: () -> Unit,
) {
    composable<ChatsRoute> {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            viewModel.refreshConversations()
        }

        val conversations by viewModel.conversations.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

        ChatListScreen(
            conversations = conversations,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onConversationClick = { convId -> onNavigateToDetail(convId) },
            onNavigateToSearch = onNavigateToSearch,
            onRefresh = { viewModel.refreshConversations() },
            onErrorDismiss = { viewModel.clearError() }
        )
    }

    composable<SearchRoute> {
        val query by searchViewModel.query.collectAsStateWithLifecycle()
        val conversations by searchViewModel.filteredConversations.collectAsStateWithLifecycle()
        val searchedUsers by searchViewModel.searchedUsers.collectAsStateWithLifecycle()
        val errorMessage by searchViewModel.errorMessage.collectAsStateWithLifecycle()

        SearchScreen(
            query = query,
            conversations = conversations,
            searchedUsers = searchedUsers,
            errorMessage = errorMessage,
            onBackClick = onBack,
            onSearchQueryChange = { q -> searchViewModel.onQueryChange(q) },
            onConversationClick = { convId -> onNavigateToDetail(convId) },
            onStartConversation = { recipientId ->
                searchViewModel.startConversation(recipientId) { convId ->
                    onNavigateToDetail(convId)
                }
            },
            onErrorDismiss = { searchViewModel.clearError() }
        )
    }

    composable<ChatDetailRoute> { backStackEntry ->
        val args = backStackEntry.toRoute<ChatDetailRoute>()

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            viewModel.selectConversation(args.conversationId)
        }

        val conversation by viewModel.currentConversation.collectAsStateWithLifecycle()
        val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
        val replyingMessage by viewModel.replyingMessage.collectAsStateWithLifecycle()
        val isRecipientTyping by viewModel.isRecipientTyping.collectAsStateWithLifecycle()
        val currentUserId = viewModel.currentUserId

        val activeConversation = conversation
            ?: viewModel.getConversation(args.conversationId)
            ?: Conversation(
                id = args.conversationId,
                recipient = User(
                    id = "",
                    name = "Trò chuyện",
                    avatarUrl = ""
                ),
                lastMessage = Message(
                    id = "default",
                    senderId = "",
                    text = "",
                    timestamp = System.currentTimeMillis()
                ),
                unreadCount = 0
            )

        ChatDetailScreen(
            conversation = activeConversation,
            messages = messages,
            currentUserId = currentUserId,
            replyingMessage = replyingMessage,
            isRecipientTyping = isRecipientTyping,
            onBackClick = onBack,
            onSendMessage = { text -> viewModel.sendMessage(text) },
            onSendMedia = { bytes, fileName, mediaType ->
                viewModel.sendMediaMessage(bytes, fileName, mediaType)
            },
            onCancelReply = { viewModel.setReplyingMessage(null) },
            onReplyMessage = { msg -> viewModel.setReplyingMessage(msg) },
            onUnsendMessage = { msg -> viewModel.unsendMessage(msg) },
            onTyping = { isTyping -> viewModel.sendTypingIndicator(isTyping) },
            onLoadOlderMessages = { viewModel.loadOlderMessages() }
        )
    }
}
