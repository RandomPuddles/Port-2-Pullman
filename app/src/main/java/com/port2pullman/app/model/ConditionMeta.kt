package com.port2pullman.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.port2pullman.app.data.ConditionRegistry

/**
 * Rendering metadata for condition types.
 * Delegates to [ConditionRegistry] which is loaded from res/raw/conditions.json.
 */
object ConditionMeta {

    data class Meta(
        val hasNum: Boolean = false,
        val unit: String = "",
        val placeholder: String = ""
    )

    fun get(type: String): Meta {
        val def = ConditionRegistry.getMeta(type)
        return Meta(
            hasNum = def.hasNum,
            unit = def.unit,
            placeholder = def.placeholder
        )
    }

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
