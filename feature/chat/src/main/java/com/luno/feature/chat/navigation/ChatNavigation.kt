package com.luno.feature.chat.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.luno.feature.chat.ChatDetailScreen
import com.luno.feature.chat.ChatListScreen
import com.luno.feature.chat.ChatViewModel
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
        ChatListScreen(
            conversations = conversations,
            onConversationClick = { convId -> onNavigateToDetail(convId) }
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
