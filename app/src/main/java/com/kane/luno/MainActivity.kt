package com.kane.luno

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kane.luno.di.AppModule
import com.kane.luno.notification.LunoFirebaseMessagingService
import com.kane.luno.ui.LunoApp
import com.kane.luno.ui.theme.LunoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.initialize(applicationContext)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            LunoTheme {
                LunoApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val conversationId =
            intent?.getStringExtra(LunoFirebaseMessagingService.EXTRA_CONVERSATION_ID)
        if (!conversationId.isNullOrBlank()) {
            // TODO: Điều hướng NavController hoặc cập nhật ViewModel để mở phòng chat:
            // navController.navigate("chat/$conversationId")
        }
    }
}
