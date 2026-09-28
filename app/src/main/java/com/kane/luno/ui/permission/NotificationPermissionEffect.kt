package com.kane.luno.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun NotificationPermissionEffect(
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }

    // Launcher xin quyền
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showRationaleDialog = false
        if (isGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (isGranted) {
                onPermissionGranted()
            } else {
                showRationaleDialog = true
            }
        } else {
            // Android 12 trở xuống không cần xin quyền runtime
            onPermissionGranted()
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "Bật thông báo tin nhắn")
            },
            text = {
                Text(
                    text = "Cho phép Luno gửi thông báo để bạn luôn nhận được tin nhắn mới nhất và không bỏ lỡ cuộc trò chuyện nào."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text(text = "Cho phép")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationaleDialog = false }
                ) {
                    Text(text = "Để sau")
                }
            }
        )
    }
}