package com.luno.feature.chat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class ZaloTab(val route: String, val title: String, val icon: ImageVector) {
    object Chats : ZaloTab("chats", "Tin nhắn", Icons.Default.Chat)
    object Contacts : ZaloTab("contacts", "Danh bạ", Icons.Default.Contacts)
    object Discover : ZaloTab("discover", "Khám phá", Icons.Default.CompassCalibration)
    object Profile : ZaloTab("profile", "Cá nhân", Icons.Default.Person)
}

@Composable
fun ZaloMainScreen(
    viewModel: ChatViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf<ZaloTab>(ZaloTab.Chats) }
    val conversations by viewModel.conversations.collectAsState()
    val selectedConvId by viewModel.selectedConversationId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()

    if (selectedConvId != null) {
        val conversation = viewModel.getConversation(selectedConvId!!)
        if (conversation != null) {
            ChatDetailScreen(
                conversation = conversation,
                messages = messages,
                onBackClick = { viewModel.selectConversation(null) },
                onSendMessage = { text -> viewModel.sendMessage(text) }
            )
            return
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = ZaloBlue
            ) {
                val tabs = listOf(ZaloTab.Chats, ZaloTab.Contacts, ZaloTab.Discover, ZaloTab.Profile)
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ZaloBlue,
                            selectedTextColor = ZaloBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.White
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                is ZaloTab.Chats -> {
                    ChatListScreen(
                        conversations = conversations,
                        onConversationClick = { convId -> viewModel.selectConversation(convId) }
                    )
                }
                is ZaloTab.Contacts -> PlaceholderScreen(title = "Danh bạ")
                is ZaloTab.Discover -> PlaceholderScreen(title = "Khám phá")
                is ZaloTab.Profile -> PlaceholderScreen(title = "Cá nhân")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, fontSize = 20.sp, color = Color.Gray)
    }
}
