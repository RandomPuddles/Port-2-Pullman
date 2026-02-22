package com.port2pullman.app.model

/**
 * A user-created custom condition with a refresh frequency.
 */
data class CustomCondition(
    val id: Long = 0,
    val title: String,
    val statement: String,
    val refreshFrequency: RefreshFrequency
)

data class RefreshFrequency(
    val value: Int,
    val unit: TimeUnit
)

enum class TimeUnit {
    SECONDS, MINUTES, HOURS, DAYS
}
