package com.port2pullman.app.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.port2pullman.app.model.*
import com.port2pullman.app.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: AlarmListViewModel,
    onCreateAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onAiCreate: () -> Unit,
    onOpenDebug: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            // Debug Console FAB – temporary purple button
            SmallFloatingActionButton(
                onClick = onOpenDebug,
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Terminal, contentDescription = "Debug Console")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Top Bar ─────────────────────────────────────
            AnimatedContent(
                targetState = state.selectMode,
                label = "topBar"
            ) { selectMode ->
                if (selectMode) {
                    SelectTopBar(
                        count = state.selectedIds.size,
                        onClose = { viewModel.exitSelectMode() },
                        onEnableAll = { viewModel.bulkEnable() },
                        onDisableAll = { viewModel.bulkDisable() },
                        onDeleteAll = { viewModel.requestBulkDelete() },
                    )
                } else {
                    NormalTopBar(
                        onSearch = { viewModel.showSearch() },
                        onAi = onAiCreate,
                        onCreate = onCreateAlarm,
                    )
                }
            }

            // ─── Search Bar ──────────────────────────────────
            AnimatedVisibility(visible = state.searchVisible && !state.selectMode) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onClose = { viewModel.hideSearch() },
                )
            }

            // ─── Alarm List ──────────────────────────────────
            if (state.alarms.isEmpty()) {
                EmptyState(hasFilter = state.searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            selectMode = state.selectMode,
                            isSelected = alarm.id in state.selectedIds,
                            onTap = {
                                if (state.selectMode) viewModel.toggleSelection(alarm.id)
                                else onEditAlarm(alarm.id)
                            },
                            onLongPress = {
                                if (!state.selectMode) viewModel.enterSelectMode(alarm.id)
                            },
                            onToggle = { viewModel.toggleEnabled(alarm) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    // ─── Delete Confirmation Dialog ──────────────────────────
    if (state.showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = state.deleteConfirmTitle,
            message = state.deleteConfirmMsg,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() },
        )
    }
}

/* ──────────────────────────────────────────────────────────── */
/*  Sub-composables                                            */
/* ──────────────────────────────────────────────────────────── */

@Composable
private fun NormalTopBar(
    onSearch: () -> Unit,
    onAi: () -> Unit,
    onCreate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Eventually",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.015.sp
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
        IconButton(onClick = onAi) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Create")
        }
        IconButton(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = "Create Alarm")
        }
    }
}

@Composable
private fun SelectTopBar(
    count: Int,
    onClose: () -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryContainer)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = OnPrimaryContainer)
        }
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            color = OnPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEnableAll) {
            Icon(Icons.Default.Alarm, contentDescription = "Enable selected", tint = OnPrimaryContainer)
        }
        IconButton(onClick = onDisableAll) {
            Icon(Icons.Outlined.AlarmOff, contentDescription = "Disable selected", tint = OnPrimaryContainer)
        }
        IconButton(onClick = onDeleteAll) {
            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = Error)
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by title or condition…", fontSize = 15.sp) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (hasFilter) Icons.Outlined.SearchOff else Icons.Outlined.AlarmOff,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .alpha(0.5f),
            tint = Outline
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasFilter) "No matching alarms" else "No alarms yet",
            color = Outline
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasFilter) "Try a different search" else "Tap + to create one",
            fontSize = 13.sp,
            color = Outline
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    selectMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val condText = buildConditionSummary(alarm.rootCondition)
    val cardAlpha = if (alarm.enabled) 1f else 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryContainer else SurfaceCard
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alarm.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    condText.ifBlank { "No conditions" },
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                // Badges
                val badges = buildList {
                    if (alarm.readout) add("Readout" to false)
                    if (alarm.ring) add("Ring" to false)
                    if (alarm.triggerOnce) add("Once" to true)
                }
                if (badges.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        badges.forEach { (label, muted) ->
                            Badge(label, muted)
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            if (selectMode) {
                // Selection check circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Primary else androidx.compose.ui.graphics.Color.Transparent)
                        .then(
                            if (!isSelected) Modifier
                                .background(androidx.compose.ui.graphics.Color.Transparent)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color.Transparent)
                                .then(
                                    Modifier.padding(2.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(OutlineVariant.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            } else {
                // Enable/Disable switch
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryContainer,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = SurfaceCard,
                        uncheckedTrackColor = OutlineVariant,
                    )
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, muted: Boolean) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = if (muted) SurfaceVariant else PrimaryContainer,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (muted) OnSurfaceVariant else OnPrimaryContainer
        )
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Error) },
        title = { Text(title) },
        text = { Text(message, fontSize = 14.sp, color = OnSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        }
    )
}

/* ──────────────────────────────────────────────────────────── */
/*  Helpers                                                     */
/* ──────────────────────────────────────────────────────────── */

/** Build a human-readable summary from a condition tree. */
private fun buildConditionSummary(condition: Condition): String = when (condition) {
    is LeafCondition -> {
        val meta = ConditionMeta.get(condition.type)
        val valPart = if (meta.hasNum && condition.value != null) {
            val v = when (val raw = condition.value) {
                is Number -> {
                    val d = raw.toDouble()
                    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                }
                else -> raw.toString()
            }
            " $v ${meta.unit}"
        } else ""
        val prefix = if (condition.negated) "NOT " else ""
        prefix + condition.label + valPart
    }
    is CompositeCondition -> {
        val sep = " ${condition.operator.name} "
        condition.children.joinToString(sep) { buildConditionSummary(it) }
    }
}
