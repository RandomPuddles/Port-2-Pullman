package com.example.eventtriggeralarm.domain.condition.leaf.system

import android.content.Context
import java.util.Calendar
import java.util.UUID

/**
 * Checks whether the current time is within a given range or matches a day of week.
 * No permissions required.
 */
data class TimeCondition(
    override val id: String = UUID.randomUUID().toString(),
    val mode: TimeMode,
    val fromHour: Int = 0,
    val fromMinute: Int = 0,
    val toHour: Int = 23,
    val toMinute: Int = 59,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val ctx: Context
) : SystemLeafCondition(ctx) {

    enum class TimeMode { IN_RANGE, OUT_OF_RANGE, DAY_OF_WEEK }

    override val label: String
        get() = when (mode) {
            TimeMode.IN_RANGE -> "Time between $fromHour:${fromMinute.toString().padStart(2, '0')} – $toHour:${toMinute.toString().padStart(2, '0')}"
            TimeMode.OUT_OF_RANGE -> "Time outside $fromHour:${fromMinute.toString().padStart(2, '0')} – $toHour:${toMinute.toString().padStart(2, '0')}"
            TimeMode.DAY_OF_WEEK -> "Day of week in $daysOfWeek"
        }

    override suspend fun getCondition(skipCustom: Boolean): Boolean {
        val cal = Calendar.getInstance()
        return when (mode) {
            TimeMode.IN_RANGE, TimeMode.OUT_OF_RANGE -> {
                val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val fromMinutes = fromHour * 60 + fromMinute
                val toMinutes = toHour * 60 + toMinute
                val inRange = currentMinutes in fromMinutes..toMinutes
                if (mode == TimeMode.IN_RANGE) inRange else !inRange
            }
            TimeMode.DAY_OF_WEEK -> {
                val today = cal.get(Calendar.DAY_OF_WEEK)
                val adjusted = if (today == Calendar.SUNDAY) 7 else today - 1
                adjusted in daysOfWeek
            }
        }
    }
}
