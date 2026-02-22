package com.example.eventually.ui.navigation

sealed class ClockRoute(val route: String) {
    data object Alarm : ClockRoute("alarm")
    data object AlarmAdd : ClockRoute("alarm_add")
    data object WorldClock : ClockRoute("world_clock")
    data object Stopwatch : ClockRoute("stopwatch")
    data object Timer : ClockRoute("timer")
    data object Event : ClockRoute("event")
    data object EventAdd : ClockRoute("event_add")
    data object EventEdit : ClockRoute("event_edit/{eventId}")
}
