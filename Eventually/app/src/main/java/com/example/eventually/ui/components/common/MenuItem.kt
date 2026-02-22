package com.example.eventually.ui.components.common

/**
 * Represents a single item in an overflow/dropdown menu.
 * Each section (Alarm, World clock, etc.) defines its own menu items.
 *
 * @param enabled When false, the item is shown but disabled (greyed out, not clickable).
 *                Use for context-dependent items (e.g. Edit when no alarms).
 */
data class MenuItem(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)
