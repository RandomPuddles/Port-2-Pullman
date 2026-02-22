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
import com.example.eventually.domain.model.AlarmItem
import com.example.eventually.ui.components.alarm.AlarmEmptyState
import com.example.eventually.ui.components.alarm.AlarmHeader
import com.example.eventually.ui.components.alarm.AlarmHeaderState
import com.example.eventually.ui.components.alarm.AlarmList
import com.example.eventually.ui.screens.alarm.alarmMenuItems

@Composable
fun AlarmScreen(
    alarms: MutableList<AlarmItem>,
    onAddAlarm: () -> Unit = {},
) {
    var isEditMode by remember { mutableStateOf(false) }
    val selectedAlarmIds = remember { mutableStateSetOf<String>() }

    val headerState = remember(alarms.size) {
        val first = alarms.firstOrNull()
        AlarmHeaderState(
            nextAlarmSummary = first?.let { "Alarm in 58 minutes" },
            nextAlarmDetail = first?.let { "${it.label}, ${it.time}" }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AlarmHeader(
            state = headerState,
            onAddClick = onAddAlarm,
            menuItems = alarmMenuItems(
                onEdit = { isEditMode = true },
                hasAlarms = alarms.isNotEmpty()
            ),
            isEditMode = isEditMode,
            selectedCount = selectedAlarmIds.size,
            onDoneClick = {
                isEditMode = false
                selectedAlarmIds.clear()
            },
            onDeleteSelectedClick = {
                alarms.removeAll { it.id in selectedAlarmIds }
                selectedAlarmIds.clear()
                isEditMode = false
            }
        )

        if (alarms.isEmpty()) {
            AlarmEmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            AlarmList(
                alarms = alarms,
                onToggle = { alarm, enabled ->
                    val index = alarms.indexOfFirst { it.id == alarm.id }
                    if (index >= 0) {
                        alarms[index] = alarm.copy(isEnabled = enabled)
                    }
                },
                isEditMode = isEditMode,
                selectedAlarmIds = selectedAlarmIds,
                onSelectionToggle = { alarm ->
                    if (alarm.id in selectedAlarmIds) {
                        selectedAlarmIds.remove(alarm.id)
                    } else {
                        selectedAlarmIds.add(alarm.id)
                    }
                }
            )
        }
    }
}
