package com.kane.luno.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kane.luno.di.AppModule
import com.luno.core.auth.navigation.LoginRoute
import com.luno.core.auth.navigation.authGraph
import com.luno.core.auth.ui.AuthViewModel
import com.luno.feature.chat.navigation.ChatDetailRoute
import com.luno.feature.chat.navigation.ChatsRoute
import com.luno.feature.chat.navigation.chatGraph
import com.luno.feature.chat.viewmodels.ChatViewModel
import com.luno.feature.profile.ProfileViewModel
import com.luno.feature.profile.navigation.ProfileRoute
import com.luno.feature.profile.navigation.profileGraph

// Navigation Tabs Setup
enum class TopLevelTab(val title: String, val icon: ImageVector) {
    Chats("Tin nhắn", Icons.AutoMirrored.Filled.Chat),
    Contacts("Danh bạ", Icons.Default.Contacts),
    Discover("Khám phá", Icons.Default.CompassCalibration),
    Profile("Cá nhân", Icons.Default.Person)
}

@Composable
fun LunoApp(
    chatViewModel: ChatViewModel = viewModel(),
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(AppModule.authRepository) as T
            }
        }
    )

    val profileViewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(AppModule.authRepository) as T
            }
        }
    )

    val isLoadingAuth by authViewModel.isLoadingAuth.collectAsState()
    val userEmail by authViewModel.userEmail.collectAsState()

    if (isLoadingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (userEmail != null) ChatsRoute else LoginRoute

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Logic for hiding bottom bar in detail screens
    val isBottomBarVisible = currentDestination?.hierarchy?.any {
        (it.route == ChatsRoute::class.qualifiedName) ||
                (it.route == ProfileRoute::class.qualifiedName) ||
                (it.route == "Contacts") ||
                (it.route == "Discover")
    } == true

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        val isSelected = when (tab) {
                            TopLevelTab.Chats -> currentDestination.hierarchy.any { it.route == ChatsRoute::class.qualifiedName }
                            TopLevelTab.Profile -> currentDestination.hierarchy.any { it.route == ProfileRoute::class.qualifiedName }
                            else -> currentDestination.hierarchy.any { it.route == tab.name }
                        }

                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = isSelected,
                            onClick = {
                                when (tab) {
                                    TopLevelTab.Chats -> navController.navigate(ChatsRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }

                                    TopLevelTab.Profile -> navController.navigate(ProfileRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }

                                    else -> navController.navigate(tab.name) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            authGraph(
                authViewModel = authViewModel,
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

            profileGraph(
                viewModel = profileViewModel,
                onSignedOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )

            // Mock other routes
            composable("Contacts") { PlaceholderScreen("Danh bạ") }
            composable("Discover") { PlaceholderScreen("Khám phá") }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}
