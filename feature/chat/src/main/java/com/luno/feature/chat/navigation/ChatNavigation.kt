package com.luno.feature.chat.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.luno.feature.chat.ui.ChatDetailScreen
import com.luno.feature.chat.ui.ChatListScreen
import com.luno.feature.chat.viewmodels.ChatViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ChatsRoute

@Serializable
data class ChatDetailRoute(val conversationId: String)

fun NavGraphBuilder.chatGraph(
    viewModel: ChatViewModel,
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
    composable<ChatsRoute> {
        val conversations by viewModel.conversations.collectAsState()
        val searchedUsers by viewModel.searchedUsers.collectAsState()
        val isAddDialogVisible by viewModel.isAddDialogVisible.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.refreshConversations()
        }

        ChatListScreen(
            conversations = conversations,
            searchedUsers = searchedUsers,
            isAddDialogVisible = isAddDialogVisible,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onConversationClick = { convId -> onNavigateToDetail(convId) },
            onAddClick = { viewModel.setAddDialogVisible(true) },
            onDismissAddDialog = { viewModel.setAddDialogVisible(false) },
            onSearchUser = { query -> viewModel.searchUsers(query) },
            onStartConversation = { recipientId ->
                viewModel.startConversation(recipientId) { convId ->
                    onNavigateToDetail(convId)
                }
            },
            onRefresh = { viewModel.refreshConversations() },
            onErrorDismiss = { viewModel.clearError() }
        )
    }
    composable<ChatDetailRoute> { backStackEntry ->
        val args = backStackEntry.toRoute<ChatDetailRoute>()

        LaunchedEffect(args.conversationId) {
            viewModel.selectConversation(args.conversationId)
        }

        val conversation by viewModel.currentConversation.collectAsState()
        val messages by viewModel.currentMessages.collectAsState()
        val currentUserId = viewModel.currentUserId

        conversation?.let {
            ChatDetailScreen(
                conversation = it,
                messages = messages,
                currentUserId = currentUserId,
                onBackClick = onBack,
                onSendMessage = { text -> viewModel.sendMessage(text) }
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
