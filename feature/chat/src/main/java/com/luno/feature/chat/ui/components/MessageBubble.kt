package com.luno.feature.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kane.luno.utils.DateUtils
import com.luno.core.domain.model.Message

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    recipientAvatarUrl: String,
    onReplyMessage: (Message) -> Unit = {},
    onDeleteMessage: (Message) -> Unit = {}
) {

    val context = LocalContext.current
    var isHovered by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val avatarRequest = remember(recipientAvatarUrl) {
        ImageRequest.Builder(context)
            .data(recipientAvatarUrl)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

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
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> isHovered = true
                                PointerEventType.Exit -> isHovered = false
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = {
                        if (isHovered) isHovered = false
                    },
                    onLongClick = {
                        isHovered = !isHovered
                    }
                ),
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
            }
        }

        if (isHovered) {
            MessagePopupOption(
                timestamp = message.timestamp,
                isMe = isMe,
                onDismissRequest = { isHovered = false },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(message.text))
                },
                onReply = { onReplyMessage(message) },
                onDelete = { onDeleteMessage(message) }
            )
        }
    }
}


@Composable
fun MessagePopupOption(
    timestamp: Long,
    isMe: Boolean,
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    // Popup neo ở phía trên bong bóng chat
    Popup(
        alignment = if (isMe) Alignment.TopEnd else Alignment.TopStart,
        offset = IntOffset(x = 0, y = -110), // Đẩy popup nổi lên trên bubble, không đẩy layout
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.widthIn(min = 160.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Header hiển thị thời gian gửi chi tiết
                Text(
                    text = "Gửi lúc: ${DateUtils.formatMessageTime(timestamp)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                PopupOptionItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Sao chép",
                    onClick = {
                        onCopy()
                        onDismissRequest()
                    }
                )

                PopupOptionItem(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    title = "Trả lời",
                    onClick = {
                        onReply()
                        onDismissRequest()
                    }
                )

                if (isMe) {
                    PopupOptionItem(
                        icon = Icons.Default.Delete,
                        title = "Thu hồi / Xóa",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDelete()
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupOptionItem(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Text(
            text = title,
            fontSize = 13.sp,
            color = tint
        )
    }
}