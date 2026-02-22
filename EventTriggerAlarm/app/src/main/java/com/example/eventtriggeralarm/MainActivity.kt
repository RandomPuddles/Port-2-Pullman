package com.example.eventtriggeralarm

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventtriggeralarm.navigation.AppNavGraph
import com.example.eventtriggeralarm.ui.AppViewModel
import com.example.eventtriggeralarm.ui.theme.EventTriggerAlarmTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result not needed for basic flow */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        setContent {
            EventTriggerAlarmTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: AppViewModel = viewModel(factory = viewModelFactory)
                    AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
