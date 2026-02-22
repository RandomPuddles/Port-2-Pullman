package com.port2pullman.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.engine.AlarmEvaluatorService
import com.port2pullman.app.navigation.AppNavigation
import com.port2pullman.app.ui.theme.Port2PullmanTheme

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        DebugLog.i("MainActivity", "POST_NOTIFICATIONS permission granted=$granted")
        startEvaluatorService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionAndStartService()

        setContent {
            Port2PullmanTheme {
                AppNavigation(app = applicationContext as App)
            }
        }
    }

    private fun requestNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    DebugLog.d("MainActivity", "Notification permission already granted")
                    startEvaluatorService()
                }
                else -> {
                    DebugLog.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startEvaluatorService()
        }
    }

    private fun startEvaluatorService() {
        try {
            DebugLog.i("MainActivity", "Starting AlarmEvaluatorService")
            val intent = Intent(this, AlarmEvaluatorService::class.java)
            startForegroundService(intent)
        } catch (e: Exception) {
            DebugLog.e("MainActivity", "Failed to start evaluator service", e)
        }
    }
}