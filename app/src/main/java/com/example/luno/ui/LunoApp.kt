package com.example.luno.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luno.core.auth.navigation.LoginRoute
import com.luno.core.auth.navigation.authGraph
import com.luno.feature.chat.ChatViewModel
import com.luno.feature.chat.navigation.ChatDetailRoute
import com.luno.feature.chat.navigation.ChatsRoute
import com.luno.feature.chat.navigation.chatGraph

// Navigation Tabs Setup
enum class TopLevelTab(val title: String, val icon: ImageVector) {
    Chats("Tin nhắn", Icons.AutoMirrored.Filled.Chat),
    Contacts("Danh bạ", Icons.Default.Contacts),
    Discover("Khám phá", Icons.Default.CompassCalibration),
    Profile("Cá nhân", Icons.Default.Person)
}

val ZaloBlue = Color(0xFF0068FF)

@Composable
fun LunoApp(
    chatViewModel: ChatViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Logic for hiding bottom bar in detail screens
    val isBottomBarVisible = currentDestination?.hierarchy?.any { 
        it.route == ChatsRoute::class.qualifiedName ||
        it.route == "Contacts" || 
        it.route == "Discover" || 
        it.route == "Profile"
    } == true

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = ZaloBlue
                ) {
                    TopLevelTab.entries.forEach { tab ->
                        val isSelected = when (tab) {
                            TopLevelTab.Chats -> currentDestination?.hierarchy?.any { it.route == ChatsRoute::class.qualifiedName } == true
                            else -> currentDestination?.hierarchy?.any { it.route == tab.name } == true
                        }
                        
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = isSelected,
                            onClick = {
                                if (tab == TopLevelTab.Chats) {
                                    navController.navigate(ChatsRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    // Handle other mock tabs
                                    navController.navigate(tab.name) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
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
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = LoginRoute,
            modifier = Modifier.padding(paddingValues)
        ) {
            authGraph(
                onLoginSuccess = {
                    navController.navigate(ChatsRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                }
            )

            chatGraph(
                viewModel = chatViewModel,
                onNavigateToDetail = { convId ->
                    navController.navigate(ChatDetailRoute(convId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
            
            // Mock other routes
            composable("Contacts") { PlaceholderScreen("Danh bạ") }
            composable("Discover") { PlaceholderScreen("Khám phá") }
            composable("Profile") { PlaceholderScreen("Cá nhân") }
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
