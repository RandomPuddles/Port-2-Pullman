package com.example.eventtriggeralarm.domain.condition.leaf.system

import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Checks whether the user has an event in their calendar within a time window.
 * Requires READ_CALENDAR permission.
 */
data class CalendarCondition(
    override val id: String = UUID.randomUUID().toString(),
    val mode: CalendarMode,
    val windowMinutes: Int = 60,
    val ctx: Context
) : SystemLeafCondition(ctx) {

    enum class CalendarMode { HAS_EVENT, IS_FREE }

    override val label: String
        get() = "Calendar: ${mode.name.replace('_', ' ').lowercase()} in next ${windowMinutes}min"

    override suspend fun getCondition(skipCustom: Boolean): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val windowEnd = now + (windowMinutes * 60 * 1000L)

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val args = arrayOf(now.toString(), windowEnd.toString())

        val cursor = context.contentResolver.query(uri, projection, selection, args, null)
        val hasEvent = (cursor?.count ?: 0) > 0
        cursor?.close()

        when (mode) {
            CalendarMode.HAS_EVENT -> hasEvent
            CalendarMode.IS_FREE -> !hasEvent
        }
    }
}
