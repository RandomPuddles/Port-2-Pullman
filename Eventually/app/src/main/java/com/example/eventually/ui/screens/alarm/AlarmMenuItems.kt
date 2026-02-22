package com.example.eventually.ui.screens.alarm

import com.example.eventually.ui.components.common.MenuItem

/**
 * Menu items for the Alarm section overflow menu.
 * MVP: Edit only. Extensible for Sort, Alarm groups, Settings, etc.
 *
 * @param hasAlarms When false, Edit is disabled (greyed out, not clickable).
 *                  Pass alarms.isNotEmpty() from the screen.
 *                  The 3-dot menu still shows; dropdown displays disabled Edit.
 */
fun alarmMenuItems(
    onEdit: () -> Unit,
    hasAlarms: Boolean = true
): List<MenuItem> = listOf(
    MenuItem(
        label = "Edit",
        onClick = onEdit,
        enabled = hasAlarms
    )
)
