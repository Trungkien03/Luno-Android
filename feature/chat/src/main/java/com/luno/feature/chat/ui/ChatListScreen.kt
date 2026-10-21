package com.luno.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luno.core.domain.model.Conversation
import com.luno.feature.chat.ui.components.ConversationItem
import com.luno.feature.chat.ui.components.RichEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    conversations: List<Conversation>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onConversationClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onRefresh: () -> Unit = {},
    onErrorDismiss: () -> Unit = {}
) {
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            title = { Text("Thông báo") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = onErrorDismiss) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSearch() }
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    enabled = false,
                    placeholder = { Text("Tìm kiếm người dùng hoặc tin nhắn...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onNavigateToSearch) {
                Icon(Icons.Default.Add, contentDescription = "Tìm kiếm / Thêm")
            }
        }

        HorizontalDivider()

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (conversations.isEmpty()) {
                RichEmptyState(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Chưa có cuộc trò chuyện nào",
                    subtitle = "Kéo xuống để làm mới hoặc bấm nút bên trên để tìm kiếm người dùng mới",
                    actionText = "Tìm kiếm người dùng",
                    onActionClick = onNavigateToSearch
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(conversations) { conv ->
                        ConversationItem(conversation = conv) { onConversationClick(conv.id) }
                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                    }
                }
            }
        }
    }
}
