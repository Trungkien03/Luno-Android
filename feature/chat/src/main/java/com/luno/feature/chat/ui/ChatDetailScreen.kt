package com.luno.feature.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kane.luno.utils.AudioRecorder
import com.kane.luno.utils.DateUtils
import com.luno.core.domain.model.Conversation
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.model.Message
import com.luno.feature.chat.ui.components.FullscreenImageViewer
import com.luno.feature.chat.ui.components.MessageBubble
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    messages: List<Message>,
    currentUserId: String?,
    replyingMessage: Message? = null,
    isRecipientTyping: Boolean = false,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendMedia: (bytes: ByteArray, fileName: String, mediaType: MediaType) -> Unit = { _, _, _ -> },
    onCancelReply: () -> Unit = {},
    onReplyMessage: (Message) -> Unit = {},
    onUnsendMessage: (Message) -> Unit = {},
    onTyping: (Boolean) -> Unit = {},
    onLoadOlderMessages: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var textInput by remember { mutableStateOf("") }
    var isSearchModeActive by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    var isRecordingAudio by remember { mutableStateOf(false) }
    val audioRecorder = remember { AudioRecorder(context) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val audioFile = audioRecorder.startRecording()
            if (audioFile != null) {
                isRecordingAudio = true
            }
        }
    }

    val displayMessages = if (inChatSearchQuery.isNotBlank()) {
        messages.filter { it.text.contains(inChatSearchQuery, ignoreCase = true) }
    } else {
        messages
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .filter { it == 0 }
            .collect {
                onLoadOlderMessages()
            }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            if (bytes != null) {
                onSendMedia(bytes, fileName, MediaType.IMAGE)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val fileName = "file_${System.currentTimeMillis()}"
            if (bytes != null) {
                onSendMedia(bytes, fileName, MediaType.FILE)
            }
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            if (isSearchModeActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = inChatSearchQuery,
                            onValueChange = { inChatSearchQuery = it },
                            placeholder = { Text("Tìm trong trò chuyện...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchModeActive = false
                            inChatSearchQuery = ""
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Đóng tìm kiếm")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                            .background(
                                                MaterialTheme.colorScheme.tertiary,
                                                CircleShape
                                            )
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
                                    text = if (isRecipientTyping) "Đang soạn tin..." else conversation.recipient.lastSeenFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRecipientTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = if (isRecipientTyping) FontStyle.Italic else FontStyle.Normal
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchModeActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Tìm trong chat")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Phone, contentDescription = "Gọi điện")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video call")
                        }
                    },
                )
            }
        },
        bottomBar = {
            Column {
                if (replyingMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Đang trả lời ${if (replyingMessage.senderId == currentUserId) "chính mình" else conversation.recipient.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = replyingMessage.text.ifBlank {
                                        when (replyingMessage.mediaType) {
                                            MediaType.IMAGE -> "[Hình ảnh]"
                                            MediaType.AUDIO -> "[Ghi âm]"
                                            MediaType.FILE -> "[Tệp đính kèm]"
                                            else -> ""
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = onCancelReply) {
                                Icon(Icons.Default.Clear, contentDescription = "Hủy trả lời")
                            }
                        }
                    }
                }

                Surface(tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Ảnh")
                        }

                        IconButton(onClick = {
                            filePickerLauncher.launch("*/*")
                        }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Tệp")
                        }

                        IconButton(onClick = {
                            if (isRecordingAudio) {
                                val audioFile = audioRecorder.stopRecording()
                                isRecordingAudio = false
                                if (audioFile != null && audioFile.exists()) {
                                    val bytes = audioFile.readBytes()
                                    val fileName = "voice_${System.currentTimeMillis()}.m4a"
                                    onSendMedia(bytes, fileName, MediaType.AUDIO)
                                }
                            } else {
                                val isGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (isGranted) {
                                    val audioFile = audioRecorder.startRecording()
                                    if (audioFile != null) {
                                        isRecordingAudio = true
                                    }
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Micro",
                                tint = if (isRecordingAudio) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { newValue ->
                                textInput = newValue
                                onTyping(newValue.isNotBlank())
                            },
                            placeholder = { Text(if (isRecordingAudio) "Đang ghi âm..." else "Tin nhắn") },
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
                                onTyping(false)
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
            }
        },
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        if (displayMessages.isEmpty()) {
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
                for (i in displayMessages.indices) {
                    val message = displayMessages[i]
                    val prevMessage = if (i > 0) displayMessages[i - 1] else null
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
                            recipientAvatarUrl = conversation.recipient.avatarUrl,
                            onReplyMessage = onReplyMessage,
                            onDeleteMessage = onUnsendMessage,
                            onImageClick = { url -> fullscreenImageUrl = url }
                        )
                    }
                }
            }
        }

        if (fullscreenImageUrl != null) {
            FullscreenImageViewer(
                imageUrlList = listOf(fullscreenImageUrl!!),
                onDismiss = { fullscreenImageUrl = null }
            )
        }
    }
}
