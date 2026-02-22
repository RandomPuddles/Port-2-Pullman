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

    /**
     * Launcher for requesting multiple runtime permissions at once.
     * After the user responds, we start the evaluator service regardless
     * (the service and probes gracefully handle missing permissions).
     */
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (perm, granted) ->
            DebugLog.i("MainActivity", "$perm granted=$granted")
        }
        startEvaluatorService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissionsAndStartService()

        setContent {
            Port2PullmanTheme {
                AppNavigation(app = applicationContext as App)
            }
        }
    }

    /**
     * Collect every dangerous permission the app needs, filter to only
     * those not yet granted, and request them in a single system dialog.
     */
    private fun requestPermissionsAndStartService() {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        // POST_NOTIFICATIONS is only a runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }

        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            DebugLog.d("MainActivity", "Requesting permissions: $missing")
            permissionsLauncher.launch(missing.toTypedArray())
        } else {
            DebugLog.d("MainActivity", "All permissions already granted")
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