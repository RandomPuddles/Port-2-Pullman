package com.example.eventually.domain.model

data class AlarmItem(
    val id: String,
    val time: String,
    val label: String,
    val isEnabled: Boolean,
    val isRecurring: Boolean = false,
    val recurringDays: Set<Int> = emptySet(),
    val triggerDateMs: Long = 0L
)
