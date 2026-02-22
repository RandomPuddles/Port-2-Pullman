package com.example.eventually.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.eventually.ui.screens.AlarmScreen
import com.example.eventually.ui.screens.EventScreen
import com.example.eventually.ui.screens.StopwatchScreen
import com.example.eventually.ui.screens.TimerScreen
import com.example.eventually.ui.screens.WorldClockScreen

@Composable
fun ClockNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        modifier = modifier,
        startDestination = ClockRoute.Alarm.route
    ) {
        composable(ClockRoute.Alarm.route) {
            AlarmScreen()
        }
        composable(ClockRoute.WorldClock.route) {
            WorldClockScreen()
        }
        composable(ClockRoute.Stopwatch.route) {
            StopwatchScreen()
        }
        composable(ClockRoute.Timer.route) {
            TimerScreen()
        }
        composable(ClockRoute.Event.route) {
            EventScreen()
        }
    }
}
