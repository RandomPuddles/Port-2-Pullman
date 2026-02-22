package com.example.eventually.ui.screens.event

import com.example.eventually.ui.components.common.MenuItem

fun eventMenuItems(
    onEdit: () -> Unit,
    hasEvents: Boolean = true
): List<MenuItem> = listOf(
    MenuItem(
        label = "Edit",
        onClick = onEdit,
        enabled = hasEvents
    )
)
