package com.port2pullman.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rendering metadata for built-in condition types.
 */
object ConditionMeta {

    data class Meta(
        val hasNum: Boolean = false,
        val unit: String = "",
        val placeholder: String = ""
    )

    private val metadata = mapOf(
        "temperature_above" to Meta(true, "°F"),
        "temperature_below" to Meta(true, "°F"),
        "rain_expected" to Meta(false),
        "snow_expected" to Meta(false),
        "wind_speed_above" to Meta(true, "mph"),
        "humidity_above" to Meta(true, "%"),
        "battery_below" to Meta(true, "%"),
        "battery_above" to Meta(true, "%"),
        "connected_wifi" to Meta(false),
        "bluetooth_connected" to Meta(false),
        "charging" to Meta(false),
        "time_is" to Meta(false, placeholder = "HH:MM"),
        "day_of_week" to Meta(false),
        "date_is" to Meta(false),
        "minutes_from_now" to Meta(true, "min"),
        "arrive_at" to Meta(false),
        "leave_location" to Meta(false),
        "within_radius" to Meta(true, "mi"),
        "every_x_hours" to Meta(true, "hrs"),
        "every_x_days" to Meta(true, "days"),
        "every_x_weeks" to Meta(true, "weeks"),
        "x_times_per_day" to Meta(true, "times"),
        "x_times_per_week" to Meta(true, "times"),
    )

    fun get(type: String): Meta = metadata[type] ?: Meta(false)

    /** Map a category icon string to a Compose ImageVector. */
    fun iconForCategory(name: String): ImageVector = when (name) {
        "cloud" -> Icons.Outlined.Cloud
        "smartphone" -> Icons.Filled.PhoneAndroid
        "schedule" -> Icons.Filled.Schedule
        "location_on" -> Icons.Filled.LocationOn
        "event_repeat" -> Icons.Filled.Repeat
        "tune" -> Icons.Filled.Tune
        else -> Icons.Filled.Star
    }

    /** Map option names to icons and descriptions for setup screen. */
    data class OptionInfo(
        val icon: ImageVector,
        val title: String,
        val description: String
    )

    val readoutOption = OptionInfo(
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        title = "Readout",
        description = "TTS reads alarm title aloud"
    )

    val ringOption = OptionInfo(
        icon = Icons.Filled.Alarm,
        title = "Ring",
        description = "Sound alarm until dismissed"
    )

    val triggerOnceOption = OptionInfo(
        icon = Icons.Filled.LooksOne,
        title = "Trigger Once",
        description = "Disable after first trigger"
    )
}
