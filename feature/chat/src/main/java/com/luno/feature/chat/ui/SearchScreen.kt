package com.luno.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.User
import com.luno.feature.chat.ui.components.ConversationItem

@Composable
fun SearchScreen(
    query: String,
    conversations: List<Conversation>,
    searchedUsers: List<User>,
    errorMessage: String? = null,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onConversationClick: (String) -> Unit,
    onStartConversation: (String) -> Unit,
    onErrorDismiss: () -> Unit = {}
) {
    var manualUserId by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
            }

            OutlinedTextField(
                value = query,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Tìm tên, tin nhắn hoặc ID...") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
        }

        HorizontalDivider()

        if (query.isBlank() && !showManualInput) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tìm kiếm người dùng hoặc tin nhắn",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showManualInput = true }) {
                        Text("Nhập User ID (UUID) trực tiếp")
                    }
                }
            }
        } else if (showManualInput) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Nhập ID người dùng (UUID)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualUserId,
                    onValueChange = { manualUserId = it },
                    placeholder = { Text("VD: 498a1cb9-0883-4784-a175-...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (manualUserId.isNotBlank()) {
                            onStartConversation(manualUserId.trim())
                        }
                    },
                    enabled = manualUserId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bắt đầu trò chuyện")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showManualInput = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Hủy nhập ID thủ công")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (conversations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cuộc trò chuyện",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(conversations) { conv ->
                        ConversationItem(conversation = conv) { onConversationClick(conv.id) }
                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                    }
                }

                if (searchedUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Người dùng mới",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(searchedUsers) { user ->
                        UserSearchResultItem(user = user) {
                            onStartConversation(user.id)
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                    }
                }

                if (conversations.isEmpty() && searchedUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không tìm thấy kết quả phù hợp",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.name,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.lastSeenFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
