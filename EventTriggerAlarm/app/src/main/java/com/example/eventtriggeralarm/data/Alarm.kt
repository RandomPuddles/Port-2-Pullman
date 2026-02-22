package com.example.eventtriggeralarm.data

data class Alarm(
    val id: Long = 0,
    val title: String,
    val conditions: List<ConditionItem>,
    val operators: List<String>, // "AND" or "OR", length = conditions.size - 1
    val readout: Boolean = false,
    val ring: Boolean = false,
    val triggerOnce: Boolean = false,
    val enabled: Boolean = true
)

data class ConditionItem(
    val title: String,
    val hasNum: Boolean,
    val unit: String? = null,
    val value: Double? = null,
    val custom: Boolean = false,
    val refreshFreq: RefreshFreq? = null
)

data class RefreshFreq(
    val value: Int,
    val unit: String // "seconds", "minutes", "hours", "days"
)
