package com.example.eventtriggeralarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eventtriggeralarm.navigation.AppNavGraph
import com.example.eventtriggeralarm.ui.AppViewModel
import com.example.eventtriggeralarm.ui.theme.EventTriggerAlarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
