package com.example.eventually

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.eventually.ui.navigation.ClockBottomNav
import com.example.eventually.ui.navigation.ClockNavGraph
import com.example.eventually.ui.navigation.ClockRoute
import com.example.eventually.ui.theme.EventuallyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventuallyTheme {
                ClockApp()
            }
        }
    }
}

@Composable
fun ClockApp() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ClockBottomNav(
                currentRoute = currentRoute,
                onItemClick = { route ->
                    if (currentRoute != route.route) {
                        navController.navigate(route.route) {
                            popUpTo(ClockRoute.Alarm.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        ClockNavGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}
