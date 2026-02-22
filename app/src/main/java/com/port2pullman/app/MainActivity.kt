package com.port2pullman.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.port2pullman.app.navigation.AppNavigation
import com.port2pullman.app.ui.theme.Port2PullmanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Port2PullmanTheme {
                AppNavigation(app = applicationContext as App)
            }
        }
    }
}