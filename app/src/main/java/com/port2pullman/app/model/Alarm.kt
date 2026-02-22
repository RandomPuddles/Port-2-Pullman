package com.port2pullman.app.model

/**
 * Domain model representing a saved alarm.
 */
data class Alarm(
    val id: Long = 0,
    val title: String,
    val rootCondition: Condition,
    val readout: Boolean = false,
    val ring: Boolean = false,
    val triggerOnce: Boolean = false,
    val enabled: Boolean = true,
    val lastStartedAt: Long = System.currentTimeMillis()
)
