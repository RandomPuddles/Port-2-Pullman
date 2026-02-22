package com.example.eventually.domain.model

data class AlarmItem(
    val id: String,
    val time: String,
    val label: String,
    val title: String = "",
    val alarmSoundUri: String? = null,
    val vibration: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeIntervalMinutes: Int = 5,
    val snoozeRepeatCount: Int = 3,
    val isEnabled: Boolean,
    val isRecurring: Boolean = false,
    val recurringDays: Set<Int> = emptySet(),
    val triggerDateMs: Long = 0L
)
