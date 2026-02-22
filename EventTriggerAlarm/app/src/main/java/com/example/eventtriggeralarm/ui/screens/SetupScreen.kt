package com.example.eventtriggeralarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eventtriggeralarm.data.ConditionItem
import com.example.eventtriggeralarm.ui.AppState
import com.example.eventtriggeralarm.ui.AppViewModel
import com.example.eventtriggeralarm.ui.theme.PrimaryContainer

@Composable
fun SetupScreen(
    state: AppState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigateToAddCondition: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text(
                if (state.setupMode == com.example.eventtriggeralarm.ui.SetupMode.Edit) "Edit Alarm" else "Create Alarm",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (state.setupMode == com.example.eventtriggeralarm.ui.SetupMode.Edit) {
                IconButton(onClick = { state.setupAlarmIndex?.let { viewModel.showConfirmDeleteAlarm(it) } }) {
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            val canSave = state.setupTitle.isNotBlank() && state.setupConditions.isNotEmpty()
            Text(
                "Save",
                style = MaterialTheme.typography.labelLarge,
                color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier
                    .padding(8.dp, 16.dp)
                    .then(
                        if (canSave) Modifier.clickable {
                            viewModel.setSetupTitle(state.setupTitle)
                            viewModel.saveAlarm()
                            onSave()
                        } else Modifier
                    )
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp, 8.dp)
        ) {
            Text(
                "ALARM TITLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.setupTitle,
                onValueChange = { viewModel.setSetupTitle(it) },
                placeholder = { Text("Enter alarm title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "CONDITIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            state.setupConditions.forEachIndexed { i, cond ->
                if (i > 0) {
                    OperatorChip(
                        op = state.setupOperators.getOrElse(i - 1) { "AND" },
                        onClick = { viewModel.openBoolDialog(i - 1) }
                    )
                }
                ConditionBlock(
                    condition = cond,
                    onModify = {
                        viewModel.openAddCondition(i)
                        onNavigateToAddCondition()
                    },
                    onSetValue = { viewModel.openNumValDialog(i) },
                    onRemove = { viewModel.removeCondition(i) }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable {
                        viewModel.openAddCondition(null)
                        onNavigateToAddCondition()
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Condition", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "OPTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            OptionRow(
                icon = Icons.Filled.RecordVoiceOver,
                title = "Readout",
                desc = "TTS reads alarm title aloud",
                checked = state.setupReadout,
                onCheckedChange = { viewModel.setSetupReadout(it) }
            )
            OptionRow(
                icon = Icons.Filled.Alarm,
                title = "Ring",
                desc = "Sound alarm until dismissed",
                checked = state.setupRing,
                onCheckedChange = { viewModel.setSetupRing(it) }
            )
            OptionRow(
                icon = Icons.Filled.LooksOne,
                title = "Trigger Once",
                desc = "Disable after first trigger",
                checked = state.setupTriggerOnce,
                onCheckedChange = { viewModel.setSetupTriggerOnce(it) }
            )
        }
    }
}

@Composable
private fun OperatorChip(op: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp, 0.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(99.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            op,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ConditionBlock(
    condition: ConditionItem,
    onModify: () -> Unit,
    onSetValue: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "⋮⋮",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(4.dp)
            )
            Text(
                condition.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onModify),
                maxLines = 2
            )
            if (condition.hasNum) {
                Card(
                    modifier = Modifier
                        .clickable(onClick = onSetValue)
                        .padding(4.dp),
                    shape = RoundedCornerShape(99.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainer)
                ) {
                    Text(
                        "${condition.value ?: "—"} ${condition.unit.orEmpty()}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.Close, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
