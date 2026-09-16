package com.luno.feature.profile.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.luno.feature.profile.ProfileScreen
import com.luno.feature.profile.ProfileViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileGraph(
    viewModel: ProfileViewModel,
    onSignedOut: () -> Unit,
) {
    composable<ProfileRoute> {
        val email by viewModel.email.collectAsState()
        val userImg by viewModel.userImg.collectAsState()
        val isSigningOut by viewModel.isSigningOut.collectAsState()
        ProfileScreen(
            email = email,
            userImg = userImg,
            isSigningOut = isSigningOut,
            onSignOut = { viewModel.signOut(onSignedOut) },
        )
    }
}
