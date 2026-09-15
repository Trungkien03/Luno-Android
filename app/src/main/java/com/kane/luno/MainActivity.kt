package com.kane.luno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kane.luno.ui.LunoApp
import com.kane.luno.ui.theme.LunoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunoTheme {
                LunoApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LunoAppPreview() {
    LunoTheme {
        LunoApp()
    }
}
