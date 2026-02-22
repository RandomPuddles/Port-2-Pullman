package com.port2pullman.app.ui.setup

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.port2pullman.app.model.ConditionMeta
import com.port2pullman.app.model.LeafCondition
import com.port2pullman.app.model.Operator
import com.port2pullman.app.ui.home.ConfirmDeleteDialog
import com.port2pullman.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onAddCondition: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Surface) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Top Bar ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    if (state.isEditing) "Edit Alarm" else "Create Alarm",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                if (state.isEditing) {
                    IconButton(onClick = { viewModel.requestDeleteAlarm() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
                    }
                }
                TextButton(onClick = { viewModel.save(onBack) }) {
                    Text("Save", fontWeight = FontWeight.Medium, color = Primary)
                }
            }

            // ─── Body ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Alarm Title
                SectionLabel("Alarm Title")
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.setTitle(it) },
                    placeholder = { Text("Enter alarm title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineVariant,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                    )
                )

                Spacer(Modifier.height(24.dp))

                // Conditions
                SectionLabel("Conditions")
                ConditionsList(
                    conditions = state.conditions,
                    operators = state.operators,
                    onRemove = { viewModel.removeCondition(it) },
                    onModify = {
                        viewModel.openModifyCondition(it)
                        onAddCondition()
                    },
                    onNumVal = { viewModel.openNumVal(it) },
                    onBoolOp = { viewModel.openBoolPopup(it) },
                    onNegate = { viewModel.toggleNegation(it) },
                )

                Spacer(Modifier.height(12.dp))

                // Add Condition button
                OutlinedButton(
                    onClick = {
                        viewModel.openAddCondition()
                        onAddCondition()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, OutlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Condition")
                }

                Spacer(Modifier.height(24.dp))

                // Options
                HorizontalDivider(color = OutlineVariant)
                Spacer(Modifier.height(20.dp))
                SectionLabel("Options")
                OptionRow(
                    info = ConditionMeta.readoutOption,
                    checked = state.readout,
                    onToggle = { viewModel.toggleReadout() }
                )
                HorizontalDivider(color = SurfaceVariant)
                OptionRow(
                    info = ConditionMeta.ringOption,
                    checked = state.ring,
                    onToggle = { viewModel.toggleRing() }
                )
                HorizontalDivider(color = SurfaceVariant)
                OptionRow(
                    info = ConditionMeta.triggerOnceOption,
                    checked = state.triggerOnce,
                    onToggle = { viewModel.toggleTriggerOnce() }
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ─── Popups ──────────────────────────────────────────────

    // Numerical value popup
    if (state.showNumValPopup) {
        val condIdx = state.numValCondIndex
        val cond = state.conditions.getOrNull(condIdx)
        if (cond != null) {
            NumValDialog(
                condLabel = cond.label,
                unit = ConditionMeta.get(cond.type).unit,
                currentValue = (cond.value as? Number)?.toDouble(),
                onSave = { viewModel.setNumVal(it) },
                onDismiss = { viewModel.closeNumVal() }
            )
        }
    }

    // Boolean operator popup
    if (state.showBoolPopup) {
        BoolOpSheet(
            onSelect = { viewModel.setBoolOp(it) },
            onDismiss = { viewModel.closeBoolPopup() }
        )
    }

    // Delete alarm confirmation
    if (state.showDeleteConfirm) {
        val alarmTitle = state.title.ifBlank { "this alarm" }
        ConfirmDeleteDialog(
            title = "Delete alarm?",
            message = "\"$alarmTitle\" will be permanently deleted.",
            onConfirm = { viewModel.confirmDeleteAlarm(onBack) },
            onDismiss = { viewModel.cancelDeleteAlarm() }
        )
    }
}

/* ──────────────────────────────────────────────────────────── */
/*  Sub-composables                                            */
/* ──────────────────────────────────────────────────────────── */

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = OnSurfaceVariant,
        letterSpacing = 0.06.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ConditionsList(
    conditions: List<LeafCondition>,
    operators: List<Operator>,
    onRemove: (Int) -> Unit,
    onModify: (Int) -> Unit,
    onNumVal: (Int) -> Unit,
    onBoolOp: (Int) -> Unit,
    onNegate: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        conditions.forEachIndexed { i, cond ->
            // Bool operator chip between conditions
            if (i > 0) {
                val op = operators.getOrElse(i - 1) { Operator.AND }
                BoolChip(op = op, onClick = { onBoolOp(i - 1) })
            }

            // Condition block
            ConditionBlock(
                condition = cond,
                onTapText = { onModify(i) },
                onTapNumVal = { onNumVal(i) },
                onRemove = { onRemove(i) },
                onNegate = { onNegate(i) },
            )
        }
    }
}

@Composable
private fun BoolChip(op: Operator, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(99.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary),
        color = SurfaceCard,
    ) {
        Text(
            op.name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            letterSpacing = 0.05.sp
        )
    }
}

@Composable
private fun ConditionBlock(
    condition: LeafCondition,
    onTapText: () -> Unit,
    onTapNumVal: () -> Unit,
    onRemove: () -> Unit,
    onNegate: () -> Unit,
) {
    val meta = ConditionMeta.get(condition.type)
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 100.dp.toPx() }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Background revealed on swipe — red NOT indicator
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (condition.negated) SurfaceCard else Error.copy(alpha = 0.15f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                if (condition.negated) "REMOVE NOT" else "ADD NOT",
                modifier = Modifier.padding(start = 14.dp),
                color = Error,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }

        // Foreground card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            // Only allow dragging to the right
                            val newVal = (offsetX.value + delta).coerceAtLeast(0f)
                            offsetX.snapTo(newVal)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetX.value >= swipeThresholdPx) {
                                onNegate()
                            }
                            offsetX.animateTo(0f)
                        }
                    }
                )
                .border(
                    1.5.dp,
                    if (condition.negated) Error.copy(alpha = 0.6f) else OutlineVariant,
                    RoundedCornerShape(16.dp)
                )
                .background(SurfaceCard, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            DragHandle()
            Spacer(Modifier.width(8.dp))

            // NOT badge (appears when negated)
            if (condition.negated) {
                Text(
                    "NOT",
                    color = Error,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(Error.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(6.dp))
            }

            // Condition label (tappable to modify)
            Text(
                condition.label,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTapText() },
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Numerical value badge
            if (meta.hasNum) {
                Spacer(Modifier.width(4.dp))
                val displayVal = when (val v = condition.value) {
                    is Number -> {
                        val d = v.toDouble()
                        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                    }
                    null -> "—"
                    else -> v.toString()
                }
                Surface(
                    modifier = Modifier.clickable { onTapNumVal() },
                    shape = RoundedCornerShape(99.dp),
                    color = PrimaryContainer,
                ) {
                    Text(
                        "$displayVal ${meta.unit}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnPrimaryContainer
                    )
                }
            }

            // Remove button
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DragHandle() {
    Column(
        modifier = Modifier.width(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 2x3 dot grid
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Outline)
                    )
                }
            }
            if (it < 2) Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun OptionRow(
    info: ConditionMeta.OptionInfo,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            info.icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(info.title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(info.description, fontSize = 12.sp, color = OnSurfaceVariant)
        }
        Switch(
            checked = checked,
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

/* ──────────────────────────────────────────────────────────── */
/*  Dialogs                                                     */
/* ──────────────────────────────────────────────────────────── */

@Composable
private fun NumValDialog(
    condLabel: String,
    unit: String,
    currentValue: Double?,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentValue?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Value") },
        text = {
            Column {
                Text("$condLabel ($unit)", fontSize = 12.sp, color = OnSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Enter value") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    text.toDoubleOrNull()?.let { onSave(it) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoolOpSheet(
    onSelect: (Operator) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard
    ) {
        Text(
            "Select Operator",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Operator.entries.forEach { op ->
            TextButton(
                onClick = { onSelect(op) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(op.name, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
