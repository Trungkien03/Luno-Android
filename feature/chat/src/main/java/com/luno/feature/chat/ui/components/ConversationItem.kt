package com.luno.feature.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.luno.core.domain.model.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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