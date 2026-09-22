package com.luno.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    conversations: List<Conversation>,
    searchedUsers: List<User>,
    isAddDialogVisible: Boolean,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onConversationClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onSearchUser: (String) -> Unit,
    onStartConversation: (String) -> Unit,
    onRefresh: () -> Unit = {},
    onErrorDismiss: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = conversations.filter {
        it.recipient.name.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.text.contains(searchQuery, ignoreCase = true)
    }

    val newUsersToChat = if (searchQuery.isNotBlank()) {
        searchedUsers.filter { user ->
            conversations.none { it.recipient.id == user.id } &&
                    (user.name.contains(searchQuery, ignoreCase = true) ||
                            user.id.contains(searchQuery, ignoreCase = true))
        }
    } else {
        emptyList()
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            title = { Text("Lỗi tạo cuộc trò chuyện") },
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
        // Search bar and actions header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchUser(it)
                },
                placeholder = { Text("Tìm kiếm cuộc trò chuyện hoặc người dùng...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Thêm")
            }
        }

        HorizontalDivider()

        if (isLoading && conversations.isEmpty() && searchQuery.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (conversations.isEmpty() && searchQuery.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Chưa có cuộc trò chuyện nào",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thêm người dùng mới để bắt đầu nhắn tin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bắt đầu trò chuyện")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredConversations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cuộc trò chuyện",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredConversations) { conv ->
                        ConversationItem(conversation = conv) { onConversationClick(conv.id) }
                        HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                    }
                }

                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = "Người dùng",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (newUsersToChat.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Không tìm thấy người dùng mới phù hợp",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(newUsersToChat) { user ->
                            UserSearchItem(user = user) {
                                onStartConversation(user.id)
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                        }
                    }
                }
            }
        }

        if (isAddDialogVisible) {
            AddConversationDialog(
                searchedUsers = searchedUsers,
                onDismiss = onDismissAddDialog,
                onSearch = onSearchUser,
                onSelectUser = onStartConversation
            )
        }
    }
}

@Composable
fun UserSearchItem(
    user: User,
    onClick: () -> Unit
) {
    val displayName = user.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp)) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AddConversationDialog(
    searchedUsers: List<User>,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectUser: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var manualUserId by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm cuộc trò chuyện mới") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                if (!showManualInput) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onSearch(it)
                        },
                        placeholder = { Text("Tìm theo tên...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showManualInput = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Nhập User ID thủ công")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchedUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Không tìm thấy người dùng",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(searchedUsers) { user ->
                                val displayName = user.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectUser(user.id) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = displayName,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = manualUserId,
                        onValueChange = { manualUserId = it },
                        placeholder = { Text("Nhập ID người dùng (UUID)...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showManualInput = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Tìm kiếm theo tên")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { if (manualUserId.isNotBlank()) onSelectUser(manualUserId.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualUserId.isNotBlank()
                    ) {
                        Text("Bắt đầu trò chuyện")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = timeFormat.format(Date(conversation.lastMessage.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online status
        Box(modifier = Modifier.size(52.dp)) {
            AsyncImage(
                model = conversation.recipient.avatarUrl,
                contentDescription = conversation.recipient.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            if (conversation.recipient.isActuallyOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and last message
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = conversation.recipient.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.unreadCount > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Time and unread badge
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelMedium,
                color = if (conversation.unreadCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (conversation.unreadCount > 0) {
                Badge {
                    Text(text = conversation.unreadCount.toString())
                }
            }
        }
    }
}
