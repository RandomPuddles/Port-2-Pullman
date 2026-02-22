package com.example.eventually.ui.components.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventually.domain.model.EventItem

@Composable
fun EventList(
    events: List<EventItem>,
    onToggle: (EventItem, Boolean) -> Unit,
    onEdit: (EventItem) -> Unit = {},
    isEditMode: Boolean = false,
    selectedEventIds: Set<String> = emptySet(),
    onSelectionToggle: (EventItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                onToggle = { enabled -> onToggle(event, enabled) },
                onEdit = onEdit,
                isEditMode = isEditMode,
                isSelected = event.id in selectedEventIds,
                onSelectToggle = { onSelectionToggle(event) }
            )
        }
    }
}
