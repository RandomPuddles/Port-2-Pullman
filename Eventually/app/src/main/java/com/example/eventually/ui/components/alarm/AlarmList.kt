package com.example.eventually.ui.components.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventually.domain.model.AlarmItem

@Composable
fun AlarmList(
    alarms: List<AlarmItem>,
    onToggle: (AlarmItem, Boolean) -> Unit,
    isEditMode: Boolean = false,
    selectedAlarmIds: Set<String> = emptySet(),
    onSelectionToggle: (AlarmItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                onToggle = { enabled -> onToggle(alarm, enabled) },
                isEditMode = isEditMode,
                isSelected = alarm.id in selectedAlarmIds,
                onSelectToggle = { onSelectionToggle(alarm) }
            )
        }
    }
}
