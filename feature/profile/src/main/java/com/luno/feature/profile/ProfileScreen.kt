package com.luno.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    email: String?,
    userImg: String?,
    isSigningOut: Boolean,
    onSignOut: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cá nhân") })
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    if (!userImg.isNullOrBlank()) {
                        AsyncImage(
                            model = userImg,
                            contentDescription = "User Avatar",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                Text(
                    text = email ?: "Người dùng Luno",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Thông tin tài khoản") },
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text("Quyền riêng tư và bảo mật") },
                leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text("Thông báo") },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text("Trợ giúp") },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null
                    )
                },
            )

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isSigningOut) {
                    CircularProgressIndicator()
                } else {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Text(text = "Đăng xuất", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
