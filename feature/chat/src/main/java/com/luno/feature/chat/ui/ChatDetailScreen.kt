package com.luno.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kane.luno.utils.DateUtils
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    messages: List<Message>,
    currentUserId: String?,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val context = LocalContext.current
                        val avatarRequest = remember(conversation) {
                            ImageRequest.Builder(context)
                                .data(conversation.recipient.avatarUrl)
                                .crossfade(true)
                                .memoryCachePolicy(policy = CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        }
                        Box(modifier = Modifier.size(38.dp)) {
                            AsyncImage(
                                model = avatarRequest,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            if (conversation.recipient.isActuallyOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = conversation.recipient.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = conversation.recipient.lastSeenFormatted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở về")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Phone, contentDescription = "Gọi điện")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video call")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Image, contentDescription = "Ảnh")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Tin nhắn") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput)
                            textInput = ""
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = if (textInput.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    AsyncImage(
                        model = conversation.recipient.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = conversation.recipient.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bắt đầu cuộc trò chuyện với ${conversation.recipient.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp + paddingValues.calculateStartPadding(layoutDirection),
                    end = 12.dp + paddingValues.calculateEndPadding(layoutDirection),
                    top = 8.dp + paddingValues.calculateTopPadding(),
                    bottom = 8.dp + paddingValues.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in messages.indices) {
                    val message = messages[i]
                    val prevMessage = if (i > 0) messages[i - 1] else null
                    val showDateHeader = prevMessage == null || !DateUtils.isSameDay(
                        prevMessage.timestamp,
                        message.timestamp
                    )

                    if (showDateHeader) {
                        item(key = "date_${message.timestamp}_$i") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = DateUtils.formatHeaderDate(message.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item(key = message.id) {
                        val isMe = message.senderId == currentUserId || message.senderId == "me"
                        MessageBubble(
                            message = message,
                            isMe = isMe,
                            recipientAvatarUrl = conversation.recipient.avatarUrl
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    recipientAvatarUrl: String
) {

    val context = LocalContext.current
    val avatarRequest = remember(recipientAvatarUrl) {
        ImageRequest.Builder(context)
            .data(recipientAvatarUrl)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    val formattedTime = DateUtils.formatMessageTime(message.timestamp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            AsyncImage(
                model = avatarRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            color = if (isMe) {
                Color(0xFFE3F2FD)
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isMe) 0.dp else 1.dp,
            shadowElevation = if (isMe) 0.dp else 1.dp,
            shape = if (isMe) {
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp
                )
            } else {
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp
                )
            }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isMe) Color(0xFF1565C0).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.7f
                    ),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
