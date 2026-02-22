package com.example.eventtriggeralarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.eventtriggeralarm.data.Alarm
import com.example.eventtriggeralarm.ui.AppState
import com.example.eventtriggeralarm.ui.AppViewModel
import com.example.eventtriggeralarm.ui.components.iconForName

@Composable
fun BoolOperatorDialog(state: AppState, viewModel: AppViewModel) {
    if (!state.showBoolDialog) return
    Dialog(
        onDismissRequest = { viewModel.closeBoolDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp, 16.dp)) {
                Text("Select Operator", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                listOf("AND", "OR").forEach { op ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.setOperator(state.boolOpIndex ?: 0, op) }
                            .padding(14.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(op, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun AiPromptDialog(state: AppState, viewModel: AppViewModel, onDismiss: () -> Unit) {
    if (!state.showAiDialog) return
    Dialog(
        onDismissRequest = { viewModel.closeAiDialog(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp, 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Alarm Creator", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.closeAiDialog(); onDismiss() }) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.aiPrompt,
                    onValueChange = { viewModel.setAiPrompt(it) },
                    placeholder = { Text("Write prompt…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    minLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.generateAlarmFromAi()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.aiPrompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Generate Alarm")
                }
            }
        }
    }
}

@Composable
fun CustomConditionDialog(state: AppState, viewModel: AppViewModel) {
    if (!state.showCustomCondDialog) return
    Dialog(
        onDismissRequest = { viewModel.closeCustomCondDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp, 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (state.manageCustomIndex != null) "Modify Custom Condition" else "Create Custom Condition",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.closeCustomCondDialog() }) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Condition Title", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.customCondTitle,
                    onValueChange = { viewModel.setCustomCondTitle(it) },
                    placeholder = { Text("e.g. Battery Low") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text("Condition Statement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.customCondStmt,
                    onValueChange = { viewModel.setCustomCondStmt(it) },
                    placeholder = { Text("e.g. Battery level < 20%") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text("Refresh Frequency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                val freqHasError = state.customCondFreqVal.isNotEmpty() && !state.customCondFreqVal.all { it.isDigit() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.customCondFreqVal,
                        onValueChange = { newVal -> viewModel.setCustomCondFreqVal(newVal.filter { it.isDigit() }) },
                        placeholder = { Text("e.g. 5") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        isError = freqHasError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (freqHasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (freqHasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("seconds", "minutes", "hours", "days").forEach { unit ->
                            val selected = state.customCondFreqUnit == unit
                            val label = if (unit == "hours") "hr" else unit.take(3)
                            Text(
                                label,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.setCustomCondFreqUnit(unit) }
                                    .padding(8.dp, 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.saveCustomCondition()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.customCondTitle.isNotBlank() && !freqHasError,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Condition")
                }
            }
        }
    }
}

@Composable
fun NumValDialog(state: AppState, viewModel: AppViewModel) {
    if (!state.showNumValDialog) return
    val cond = state.numValCondIndex?.let { state.setupConditions.getOrNull(it) }
    Dialog(
        onDismissRequest = { viewModel.closeNumValDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp, 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set Value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.closeNumValDialog() }) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "${cond?.title ?: ""} (${cond?.unit ?: "value"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.numValInput,
                    onValueChange = { viewModel.setNumValInput(it) },
                    placeholder = { Text("Enter value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val v = state.numValInput.toDoubleOrNull()
                        viewModel.setConditionValue(state.numValCondIndex ?: 0, v)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Set")
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(state: AppState, viewModel: AppViewModel, onDeleted: (() -> Unit)? = null) {
    if (!state.showConfirmDelete) return
    Dialog(
        onDismissRequest = { viewModel.cancelConfirmDelete() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp, 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(state.confirmDeleteTitle, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    state.confirmDeleteMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.cancelConfirmDelete() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.executePendingDelete()
                            onDeleted?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun ManageCustomDialog(state: AppState, viewModel: AppViewModel) {
    if (!state.showManageCustomDialog) return
    val idx = state.manageCustomIndex ?: return
    val cond = state.customConditions.getOrNull(idx) ?: return
    Dialog(
        onDismissRequest = { viewModel.closeManageCustomDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp, 16.dp)) {
                Text(
                    cond.title.take(50).let { if (cond.title.length > 50) "$it…" else it },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.closeManageCustomDialog()
                            viewModel.openModifyCustomCond(idx)
                        }
                        .padding(14.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Modify")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.deleteCustomCondition(idx)
                            viewModel.closeManageCustomDialog()
                        }
                        .padding(14.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.closeManageCustomDialog() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun TriggeredDialog(state: AppState, viewModel: AppViewModel) {
    if (!state.showTriggeredDialog) return
    val alarm = state.triggeredAlarm ?: return
    Dialog(
        onDismissRequest = { viewModel.dismissTriggered() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp, 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    iconForName(state.triggeredIcon),
                    null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(alarm.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    state.triggeredMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.dismissTriggered() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}
