package com.example.eventually.ui.components.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventually.ui.components.common.MenuItem
import com.example.eventually.ui.components.common.OverflowMenu

data class AlarmHeaderState(
    val nextAlarmSummary: String?,
    val nextAlarmDetail: String?
)

@Composable
fun AlarmHeader(
    state: AlarmHeaderState,
    onAddClick: () -> Unit,
    menuItems: List<MenuItem>,
    isEditMode: Boolean = false,
    selectedCount: Int = 0,
    onDoneClick: () -> Unit = {},
    onDeleteSelectedClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = state.nextAlarmSummary ?: "No alarms",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            state.nextAlarmDetail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row {
            if (isEditMode) {
                if (selectedCount > 0) {
                    IconButton(onClick = onDeleteSelectedClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete selected"
                        )
                    }
                }
                TextButton(onClick = onDoneClick) {
                    Text("Done")
                }
            } else {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add alarm"
                    )
                }
                OverflowMenu(items = menuItems)
            }
        }
    }
}
