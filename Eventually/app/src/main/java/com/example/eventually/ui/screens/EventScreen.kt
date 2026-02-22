package com.example.eventually.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.eventually.domain.model.EventItem
import com.example.eventually.ui.components.event.EventEmptyState
import com.example.eventually.ui.components.event.EventHeader
import com.example.eventually.ui.components.event.EventHeaderState
import com.example.eventually.ui.components.event.EventList
import com.example.eventually.ui.screens.event.eventMenuItems

@Composable
fun EventScreen(
    events: MutableList<EventItem>,
    onAddEvent: () -> Unit = {},
    onEditEvent: (EventItem) -> Unit = {},
) {
    var isEditMode by remember { mutableStateOf(false) }
    val selectedEventIds = remember { mutableStateSetOf<String>() }

    val headerState = remember(events.size) {
        val first = events.firstOrNull()
        EventHeaderState(
            summary = first?.let { "${events.size} event${if (events.size == 1) "" else "s"}" },
            detail = first?.let { it.title }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EventHeader(
            state = headerState,
            onAddClick = onAddEvent,
            menuItems = eventMenuItems(
                onEdit = { isEditMode = true },
                hasEvents = events.isNotEmpty()
            ),
            isEditMode = isEditMode,
            selectedCount = selectedEventIds.size,
            onDoneClick = {
                isEditMode = false
                selectedEventIds.clear()
            },
            onDeleteSelectedClick = {
                events.removeAll { it.id in selectedEventIds }
                selectedEventIds.clear()
                isEditMode = false
            }
        )

        if (events.isEmpty()) {
            EventEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            EventList(
                events = events,
                onToggle = { event, enabled ->
                    val index = events.indexOfFirst { it.id == event.id }
                    if (index >= 0) {
                        events[index] = event.copy(isEnabled = enabled)
                    }
                },
                onEdit = onEditEvent,
                isEditMode = isEditMode,
                selectedEventIds = selectedEventIds,
                onSelectionToggle = { event ->
                    if (event.id in selectedEventIds) {
                        selectedEventIds.remove(event.id)
                    } else {
                        selectedEventIds.add(event.id)
                    }
                }
            )
        }
    }
}
