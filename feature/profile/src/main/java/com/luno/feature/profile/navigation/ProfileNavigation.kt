package com.luno.feature.profile.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.luno.feature.profile.ProfileScreen
import com.luno.feature.profile.ProfileViewModel
import com.luno.feature.profile.ui.EditProfileScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

@Serializable
data object EditProfileRoute

fun NavGraphBuilder.profileGraph(
    viewModel: ProfileViewModel,
    onSignedOut: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    composable<ProfileRoute> {
        val email by viewModel.email.collectAsStateWithLifecycle()
        val userImg by viewModel.userImg.collectAsStateWithLifecycle()
        val isSigningOut by viewModel.isSigningOut.collectAsStateWithLifecycle()

        ProfileScreen(
            email = email,
            userImg = userImg,
            isSigningOut = isSigningOut,
            onSignOut = {
                viewModel.signOut(onSignedOut)
            },
            onNavigateToEditProfile = onNavigateToEditProfile
        )
    }

    composable<EditProfileRoute> {
        val email by viewModel.email.collectAsStateWithLifecycle()
        val userImg by viewModel.userImg.collectAsStateWithLifecycle()
        val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()

        EditProfileScreen(
            currentUserId = "",
            currentEmail = email,
            currentDisplayName = email?.substringBefore("@") ?: "User",
            currentUsername = "",
            currentBio = "",
            currentAvatarUrl = userImg.orEmpty(),
            isSaving = isSavingProfile,
            onBackClick = onBack,
            onSaveProfile = { displayName, username, bio, bytes, fileName ->
                viewModel.updateProfile(displayName, username, bio, bytes, fileName) {
                    onBack()
                }
            }
        )
    }
}
