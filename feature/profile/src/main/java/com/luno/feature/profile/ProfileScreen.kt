package com.luno.feature.profile

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kane.luno.utils.CacheManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    email: String?,
    userImg: String?,
    isSigningOut: Boolean,
    onSignOut: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    var cacheSize by remember { mutableStateOf(CacheManager.getFormattedCacheSize(context)) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Dọn dẹp bộ nhớ đệm") },
            text = { Text("Xóa bộ nhớ đệm tạm thời ($cacheSize) để giải phóng dung lượng bộ nhớ thiết bị?") },
            confirmButton = {
                Button(onClick = {
                    CacheManager.clearAppCache(context)
                    cacheSize = CacheManager.getFormattedCacheSize(context)
                    showClearCacheDialog = false
                    Toast.makeText(context, "Đã dọn dẹp bộ nhớ đệm", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Dọn dẹp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("Trang cá nhân") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/800/400",
                        contentDescription = "Cover Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(92.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = 20.dp, y = (-10).dp)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    if (!userImg.isNullOrBlank()) {
                        AsyncImage(
                            model = userImg,
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                        ?: "Người dùng Luno",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email ?: "Chưa liên kết email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateToEditProfile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chỉnh sửa trang cá nhân")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Chỉnh sửa thông tin cá nhân") },
                supportingContent = { Text("Tên hiển thị, username, bio") },
                leadingContent = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { onNavigateToEditProfile() }
            )
            ListItem(
                headlineContent = { Text("Quản lý bộ nhớ đệm") },
                supportingContent = { Text("Dung lượng bộ nhớ tạm: $cacheSize") },
                leadingContent = {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { showClearCacheDialog = true }
            )
            ListItem(
                headlineContent = { Text("Quyền riêng tư và bảo mật") },
                supportingContent = { Text("Quản lý mật khẩu & xác thực") },
                leadingContent = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Thông báo & Âm thanh") },
                leadingContent = {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Trợ giúp & Hỗ trợ") },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSigningOut) {
                    CircularProgressIndicator()
                } else {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất", color = Color.Red)
                    }
                }
            }
        }
    }
}
