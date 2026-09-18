package com.luno.feature.chat.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

        ChatListScreen(
            conversations = conversations,
            searchedUsers = searchedUsers,
            isAddDialogVisible = isAddDialogVisible,
            onConversationClick = { convId -> onNavigateToDetail(convId) },
            onAddClick = { viewModel.setAddDialogVisible(true) },
            onDismissAddDialog = { viewModel.setAddDialogVisible(false) },
            onSearchUser = { query -> viewModel.searchUsers(query) },
            onStartConversation = { recipientId ->
                viewModel.startConversation(recipientId) { convId ->
                    onNavigateToDetail(convId)
                }
            }
        )
    }
    composable<ChatDetailRoute> { backStackEntry ->
        val args = backStackEntry.toRoute<ChatDetailRoute>()
        val conversation = viewModel.getConversation(args.conversationId)
        val messages by viewModel.currentMessages.collectAsState()

        conversation?.let {
            ChatDetailScreen(
                conversation = it,
                messages = messages,
                onBackClick = onBack,
                onSendMessage = { text -> viewModel.sendMessage(text) }
            )
        }
    }
}
