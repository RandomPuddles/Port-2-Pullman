package com.example.eventually.domain.model

/**
 * Event alarm - simpler than AlarmItem.
 * Title, alarm sound, vibration, recurring. No time/snooze/date.
 */
data class EventItem(
    val id: String,
    val title: String,
    val condition: String = "",
    val alarmSoundUri: String? = null,
    val vibration: Boolean = true,
    val isRecurring: Boolean = false,
    val isEnabled: Boolean
)
