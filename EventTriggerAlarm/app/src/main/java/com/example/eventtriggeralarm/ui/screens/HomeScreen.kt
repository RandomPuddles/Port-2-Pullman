package com.example.eventtriggeralarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eventtriggeralarm.data.Alarm
import com.example.eventtriggeralarm.ui.AppState
import com.example.eventtriggeralarm.ui.AppViewModel
import com.example.eventtriggeralarm.ui.theme.PrimaryContainer
import com.example.eventtriggeralarm.ui.theme.SurfaceVariant

@Composable
fun HomeScreen(
    state: AppState,
    viewModel: AppViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToAddCondition: () -> Unit,
    onOpenAiDialog: () -> Unit
) {
    val filteredAlarms = state.alarms.filter { alarm ->
        state.searchQuery.isEmpty() || alarm.title.contains(state.searchQuery, ignoreCase = true) ||
            alarm.conditions.any { it.title.contains(state.searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        if (state.selectMode) {
            SelectModeTopBar(
                count = state.selectedIndices.size,
                onClose = { viewModel.exitSelectMode() },
                onEnable = { viewModel.bulkEnable() },
                onDisable = { viewModel.bulkDisable() },
                onDelete = { viewModel.showConfirmDeleteBulk() }
            )
        } else {
            NormalTopBar(
                onSearch = { viewModel.toggleSearch() },
                onAi = onOpenAiDialog,
                onCreate = {
                    viewModel.openCreateAlarm()
                    onNavigateToSetup()
                }
            )
        }

        AnimatedVisibility(visible = state.searchVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by title or condition…") },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                IconButton(onClick = { viewModel.toggleSearch() }) {
                    Icon(Icons.Filled.Close, "Close search")
                }
            }
        }

        if (filteredAlarms.isEmpty()) {
            EmptyState(
                hasSearch = state.searchQuery.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(filteredAlarms) { idx, alarm ->
                    val actualIndex = state.alarms.indexOf(alarm)
                    AlarmCard(
                        alarm = alarm,
                        isSelectMode = state.selectMode,
                        isSelected = actualIndex in state.selectedIndices,
                        onToggle = { viewModel.toggleAlarmEnabled(actualIndex) },
                        onClick = {
                            if (state.selectMode) viewModel.toggleSelection(actualIndex)
                            else {
                                viewModel.openEditAlarm(actualIndex)
                                onNavigateToSetup()
                            }
                        },
                        onLongClick = { viewModel.enterSelectMode(actualIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NormalTopBar(
    onSearch: () -> Unit,
    onAi: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Conditional Alarms",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search") }
        IconButton(onClick = onAi) { Icon(Icons.Filled.AutoAwesome, "AI Create") }
        IconButton(onClick = onCreate) { Icon(Icons.Filled.Add, "Create Alarm") }
    }
}

@Composable
private fun SelectModeTopBar(
    count: Int,
    onClose: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(12.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, "Cancel selection")
        }
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEnable) { Icon(Icons.Filled.AlarmOn, "Enable selected") }
        IconButton(onClick = onDisable) { Icon(Icons.Filled.AlarmOff, "Disable selected") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, "Delete selected", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun EmptyState(hasSearch: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Add,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasSearch) "No matching alarms" else "No alarms yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            if (hasSearch) "Try a different search" else "Tap + to create one",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val condText = alarm.conditions.joinToString(" ${alarm.operators.firstOrNull() ?: "AND"} ") { c ->
        c.title + if (c.hasNum && c.value != null) " ${c.value} ${c.unit.orEmpty()}" else ""
    }
    val badges = buildList {
        if (alarm.readout) add("Readout")
        if (alarm.ring) add("Ring")
        if (alarm.triggerOnce) add("Once")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isSelectMode) Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                        else Modifier
                    )
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alarm.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    condText.ifEmpty { "No conditions" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (badges.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        badges.forEach { badge ->
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .background(
                                        if (badge == "Once") SurfaceVariant else PrimaryContainer,
                                        RoundedCornerShape(99.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            if (isSelectMode) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
