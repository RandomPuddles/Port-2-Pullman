package com.example.eventtriggeralarm.data

data class ConditionTemplate(
    val title: String,
    val hasNum: Boolean,
    val unit: String? = null,
    val placeholder: String? = null
)

data class Category(
    val name: String,
    val icon: String, // Material icon name
    val conditions: List<ConditionTemplate>
)

val BUILTIN_CATEGORIES = listOf(
    Category(
        name = "Weather",
        icon = "cloud",
        conditions = listOf(
            ConditionTemplate("Temperature above", true, "°F"),
            ConditionTemplate("Temperature below", true, "°F"),
            ConditionTemplate("Rain expected", false),
            ConditionTemplate("Snow expected", false),
            ConditionTemplate("Wind speed above", true, "mph"),
            ConditionTemplate("Humidity above", true, "%")
        )
    ),
    Category(
        name = "Device Attributes",
        icon = "smartphone",
        conditions = listOf(
            ConditionTemplate("Battery below", true, "%"),
            ConditionTemplate("Battery above", true, "%"),
            ConditionTemplate("Connected to WiFi", false),
            ConditionTemplate("Bluetooth connected", false),
            ConditionTemplate("Charging", false)
        )
    ),
    Category(
        name = "Time / Date",
        icon = "schedule",
        conditions = listOf(
            ConditionTemplate("Time is", false, placeholder = "HH:MM"),
            ConditionTemplate("Day of week is", false),
            ConditionTemplate("Date is", false),
            ConditionTemplate("Minutes from now", true, "min")
        )
    ),
    Category(
        name = "Location",
        icon = "location_on",
        conditions = listOf(
            ConditionTemplate("Arrive at location", false),
            ConditionTemplate("Leave location", false),
            ConditionTemplate("Within radius of", true, "mi")
        )
    ),
    Category(
        name = "Recurring Schedule",
        icon = "event_repeat",
        conditions = listOf(
            ConditionTemplate("Every X hours", true, "hrs"),
            ConditionTemplate("Every X days", true, "days"),
            ConditionTemplate("Every X weeks", true, "weeks"),
            ConditionTemplate("X times per day", true, "times"),
            ConditionTemplate("X times per week", true, "times")
        )
    ),
    Category(
        name = "Custom",
        icon = "tune",
        conditions = emptyList() // User-created, populated at runtime
    )
)
