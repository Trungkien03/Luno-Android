package com.luno.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.luno.core.model.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ZaloBlue = Color(0xFF0068FF)
val ZaloBackground = Color(0xFFF0F2F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    conversations: List<Conversation>,
    onConversationClick: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = conversations.filter {
        it.recipient.name.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.text.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Surface(
                color = ZaloBlue,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm kiếm", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        // QR Code Icon
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Code", tint = Color.White)
                        }

                        // Add Friend Icon
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color.White)
                        }
                    }
                }
            }
        },
        containerColor = ZaloBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filteredConversations) { conv ->
                ConversationItem(conversation = conv) { onConversationClick(conv.id) }
                HorizontalDivider(
                    color = Color(0xFFE4E6EB),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 76.dp)
                )
            }
        }
    }
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
            .background(Color.White)
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
            if (conversation.recipient.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF31A24C), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
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
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.dp.value.sp,
                color = Color(0xFF050505)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage.text,
                fontSize = 14.sp,
                color = if (conversation.unreadCount > 0) Color(0xFF050505) else Color(0xFF65676B),
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
                fontSize = 12.sp,
                color = if (conversation.unreadCount > 0) ZaloBlue else Color(0xFF65676B),
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (conversation.unreadCount > 0) {
                Badge(
                    containerColor = ZaloBlue,
                    contentColor = Color.White
                ) {
                    Text(text = conversation.unreadCount.toString(), fontSize = 11.sp)
                }
            }
        }
    }
}
