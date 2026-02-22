package com.port2pullman.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.port2pullman.app.model.*
import com.port2pullman.app.ui.theme.*

/**
 * Full-page screen for browsing categories and selecting a condition.
 * Mirrors the prototype's "Add Condition" page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConditionScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
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
                IconButton(onClick = {
                    if (state.selectedCategoryIndex != null) {
                        viewModel.clearCategory()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Add Condition",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
            }

            // ─── Body ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val catIdx = state.selectedCategoryIndex
                if (catIdx == null) {
                    // Category list
                    CategoryList(
                        categories = state.categories,
                        onSelect = { viewModel.selectCategory(it) }
                    )
                } else {
                    // Condition items within selected category
                    val category = state.categories.getOrNull(catIdx)
                    if (category != null) {
                        // Breadcrumb back button
                        Row(
                            modifier = Modifier
                                .clickable { viewModel.clearCategory() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                category.name,
                                color = Primary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        ConditionItemsList(
                            category = category,
                            isCustom = category.name == "Custom",
                            onSelect = { condition ->
                                viewModel.selectCondition(condition)
                                onBack()
                            },
                            onManageCustom = { idx, title ->
                                viewModel.openManageCustom(idx, title)
                            }
                        )
                    }
                }
            }

            // ─── Create Custom Condition Button ──────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.openCreateCustom() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, OutlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Custom Condition")
                }
            }
        }
    }

    // ─── Custom Condition Dialog ─────────────────────────────
    if (state.showCustomCondPopup) {
        CustomConditionDialog(
            isModify = state.customCondEditIndex != null,
            onSave = { title, stmt, freqVal, freqUnit ->
                viewModel.saveCustomCondition(title, stmt, freqVal, freqUnit)
            },
            onDismiss = { viewModel.closeCustomCondPopup() }
        )
    }

    // ─── Manage Custom Popup ─────────────────────────────────
    if (state.showManageCustomPopup) {
        ManageCustomSheet(
            title = state.manageCustomTitle,
            onModify = {
                state.manageCustomIndex?.let { viewModel.openModifyCustom(it) }
            },
            onDelete = { viewModel.deleteCustomCondition() },
            onDismiss = { viewModel.closeManageCustom() },
        )
    }
}

/* ──────────────────────────────────────────────────────────── */
/*  Sub-composables                                            */
/* ──────────────────────────────────────────────────────────── */

@Composable
private fun CategoryList(
    categories: List<Category>,
    onSelect: (Int) -> Unit,
) {
    categories.forEachIndexed { i, cat ->
        CategoryCard(cat, onClick = { onSelect(i) })
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ConditionMeta.iconForCategory(category.icon),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                category.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (category.name == "Custom" && category.conditions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = SurfaceVariant,
                ) {
                    Text(
                        "${category.conditions.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Outline
            )
        }
    }
}

@Composable
private fun ConditionItemsList(
    category: Category,
    isCustom: Boolean,
    onSelect: (LeafCondition) -> Unit,
    onManageCustom: (Int, String) -> Unit,
) {
    category.conditions.forEachIndexed { i, cond ->
        val meta = ConditionMeta.get(cond.type)
        val displayText = buildString {
            append(cond.label)
            if (meta.hasNum) append(" (${meta.unit.ifBlank { "val" }})")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.5.dp,
                    color = OutlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .let {
                    if (isCustom) it.border(
                        width = 1.5.dp,
                        color = OutlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) else it
                }
                .background(SurfaceCard)
                .clickable { onSelect(cond) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isCustom) Icons.Default.Tune else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(displayText, fontSize = 14.sp, modifier = Modifier.weight(1f))

            if (isCustom) {
                IconButton(
                    onClick = { onManageCustom(i, cond.label) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Manage",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────── */
/*  Dialogs                                                     */
/* ──────────────────────────────────────────────────────────── */

@Composable
private fun CustomConditionDialog(
    isModify: Boolean,
    onSave: (title: String, statement: String, freqVal: Int, freqUnit: TimeUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf("") }
    var freqVal by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MINUTES) }
    var unitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isModify) "Modify Custom Condition" else "Create Custom Condition") },
        text = {
            Column {
                // Title
                Text("CONDITION TITLE", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Battery Low") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Statement
                Text("CONDITION STATEMENT", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = statement,
                    onValueChange = { statement = it },
                    placeholder = { Text("e.g. Battery level < 20%") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Refresh Frequency
                Text("REFRESH FREQUENCY", fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = freqVal,
                        onValueChange = { freqVal = it },
                        placeholder = { Text("e.g. 5") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedUnit.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { unitExpanded = true },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        )
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            TimeUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        selectedUnit = unit
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val freq = freqVal.toIntOrNull() ?: 5
                        onSave(title, statement, freq, selectedUnit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Save Condition") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageCustomSheet(
    title: String,
    onModify: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        TextButton(
            onClick = onModify,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Modify", fontSize = 15.sp)
        }
        TextButton(
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Error)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Delete", fontSize = 15.sp)
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Outline)
        ) {
            Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(32.dp))
    }
}
