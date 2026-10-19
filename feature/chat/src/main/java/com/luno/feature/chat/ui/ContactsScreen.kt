package com.luno.feature.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.kane.luno.utils.DeviceContact
import com.luno.core.domain.model.FriendRequest
import com.luno.core.domain.model.User

@Composable
fun ContactsScreen(
    friendsList: List<User>,
    pendingRequests: List<FriendRequest>,
    deviceContacts: List<DeviceContact>,
    errorMessage: String? = null,
    onSyncContacts: () -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onStartConversation: (String) -> Unit,
    onErrorDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSyncContacts()
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Danh bạ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                val isGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED

                if (isGranted) {
                    onSyncContacts()
                } else {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            }) {
                Icon(Icons.Default.Sync, contentDescription = "Đồng bộ danh bạ")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Bạn bè (${friendsList.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Lời mời (${pendingRequests.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Danh bạ máy") }
            )
        }

        HorizontalDivider()

        when (selectedTab) {
            0 -> {
                if (friendsList.isEmpty()) {
                    EmptyContactsState(
                        "Chưa có bạn bè nào",
                        "Tìm kiếm người dùng hoặc đồng bộ danh bạ để thêm bạn bè"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(friendsList) { friend ->
                            FriendItem(
                                user = friend,
                                onChatClick = { onStartConversation(friend.id) })
                            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        }
                    }
                }
            }

            1 -> {
                if (pendingRequests.isEmpty()) {
                    EmptyContactsState(
                        "Chưa có lời mời kết bạn nào",
                        "Lời mời kết bạn từ người khác sẽ xuất hiện ở đây"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(pendingRequests) { request ->
                            PendingRequestItem(
                                request = request,
                                onAccept = { onAcceptRequest(request.id) },
                                onReject = { onRejectRequest(request.id) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        }
                    }
                }
            }

            2 -> {
                if (deviceContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Đồng bộ danh bạ điện thoại",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tìm bạn bè đang sử dụng Luno từ danh bạ điện thoại của bạn",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = {
                                val isGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (isGranted) {
                                    onSyncContacts()
                                } else {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            }) {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đồng bộ ngay")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(deviceContacts) { contact ->
                            DeviceContactItem(contact = contact)
                            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    user: User,
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            if (user.isActuallyOnline) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        IconButton(onClick = onChatClick) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Nhắn tin",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PendingRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = request.sender.avatarUrl,
            contentDescription = request.sender.name,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.sender.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Đã gửi lời mời kết bạn",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAccept,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Đồng ý", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onReject,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Từ chối", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun DeviceContactItem(contact: DeviceContact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = contact.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyContactsState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
