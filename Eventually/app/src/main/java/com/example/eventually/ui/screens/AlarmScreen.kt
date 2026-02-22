package com.example.eventually.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.eventually.domain.model.AlarmItem
import com.example.eventually.ui.components.alarm.AlarmEmptyState
import com.example.eventually.ui.components.alarm.AlarmHeader
import com.example.eventually.ui.components.alarm.AlarmHeaderState
import com.example.eventually.ui.components.alarm.AlarmList

@Composable
fun AlarmScreen(
    onAddAlarm: () -> Unit = {},
    onMoreOptions: () -> Unit = {}
) {
    val alarms = remember { mutableStateListOf<AlarmItem>() }
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
            onMoreClick = onMoreOptions
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
                }
            )
        }
    }
}
