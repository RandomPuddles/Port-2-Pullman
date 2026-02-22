package com.example.eventtriggeralarm.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.ui.graphics.vector.ImageVector

fun iconForName(name: String): ImageVector = when (name) {
    "search" -> Icons.Filled.Search
    "add" -> Icons.Filled.Add
    "close" -> Icons.Filled.Close
    "auto_awesome" -> Icons.Filled.AutoAwesome
    "alarm_on" -> Icons.Filled.AlarmOn
    "alarm_off", "alarm" -> Icons.Filled.Alarm
    "delete" -> Icons.Filled.Delete
    "arrow_back" -> Icons.Filled.ArrowBack
    "cloud" -> Icons.Filled.Cloud
    "smartphone" -> Icons.Filled.Smartphone
    "schedule" -> Icons.Filled.Schedule
    "location_on" -> Icons.Filled.LocationOn
    "event_repeat" -> Icons.Filled.EventRepeat
    "tune" -> Icons.Filled.Tune
    "record_voice_over" -> Icons.Filled.RecordVoiceOver
    "looks_one" -> Icons.Filled.LooksOne
    "notifications_active", "notifications" -> Icons.Filled.NotificationsActive
    "edit" -> Icons.Filled.Edit
    "check_circle_outline" -> Icons.Filled.Notifications
    else -> Icons.Filled.Add
}
