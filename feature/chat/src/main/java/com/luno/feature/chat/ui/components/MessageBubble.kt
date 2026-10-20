package com.luno.feature.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kane.luno.utils.AudioPlayer
import com.kane.luno.utils.DateUtils
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.model.Message

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    recipientAvatarUrl: String,
    onReplyMessage: (Message) -> Unit = {},
    onDeleteMessage: (Message) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isHovered by remember { mutableStateOf(false) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    val audioPlayer = remember { AudioPlayer(context) }
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
                if (message.isUnsent) {
                    Text(
                        text = "Tin nhắn đã được thu hồi",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    val replyText = message.replyToText
                    if (!replyText.isNullOrBlank()) {
                        Surface(
                            color = if (isMe) Color(0xFFBBDEFB) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(28.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = message.replyToSenderName ?: "Trả lời",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = replyText,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    when (message.mediaType) {
                        MediaType.IMAGE -> {
                            val mediaUrl = message.mediaUrl
                            if (!mediaUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Hình ảnh",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onImageClick(mediaUrl) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (message.text.isNotBlank() && message.text != "[Hình ảnh]") {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        MediaType.AUDIO -> {
                            val audioUrl = message.mediaUrl.orEmpty()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isAudioPlaying) {
                                            audioPlayer.stop()
                                            isAudioPlaying = false
                                        } else if (audioUrl.isNotBlank()) {
                                            audioPlayer.play(audioUrl) {
                                                isAudioPlaying = false
                                            }
                                            isAudioPlaying = true
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Ghi âm",
                                        tint = if (isMe) Color(0xFF0D47A1) else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (isAudioPlaying) "Đang phát..." else "Ghi âm " + (message.fileSize
                                        ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        MediaType.FILE -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = "Tệp",
                                    tint = if (isMe) Color(0xFF0D47A1) else MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = message.fileName ?: "Tệp đính kèm",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                                    )
                                    val fileSize = message.fileSize
                                    if (!fileSize.isNullOrBlank()) {
                                        Text(
                                            text = fileSize,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMe) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMe) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
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
    Popup(
        alignment = if (isMe) Alignment.TopEnd else Alignment.TopStart,
        offset = IntOffset(x = 0, y = -110),
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
