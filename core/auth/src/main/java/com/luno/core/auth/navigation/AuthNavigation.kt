package com.luno.core.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.luno.core.auth.ui.AuthViewModel
import com.luno.core.auth.ui.LoginScreen
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

fun NavGraphBuilder.authGraph(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
) {
    composable<LoginRoute> {
        LoginScreen(
            viewModel = authViewModel,
            onLoginSuccess = onLoginSuccess
        )
    }
}
