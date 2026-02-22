package com.example.eventtriggeralarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventtriggeralarm.data.BUILTIN_CATEGORIES
import com.example.eventtriggeralarm.data.Category
import com.example.eventtriggeralarm.data.ConditionItem
import com.example.eventtriggeralarm.data.ConditionTemplate
import com.example.eventtriggeralarm.ui.AppState
import com.example.eventtriggeralarm.ui.AppViewModel
import com.example.eventtriggeralarm.ui.components.iconForName
import com.example.eventtriggeralarm.ui.theme.PrimaryContainer

@Composable
fun AddConditionScreen(
    state: AppState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onConditionSelected: () -> Unit,
    onCreateCustom: () -> Unit
) {
    val categories = BUILTIN_CATEGORIES.map { cat ->
        if (cat.name == "Custom") cat.copy(conditions = state.customConditions.map { c ->
            ConditionTemplate(c.title, false)
        }) else cat
    }

    LaunchedEffect(Unit) { viewModel.selectCategory(null) }
    val selectedCatIdx = state.selectedCategoryIndex
    val showingCategoryItems = selectedCatIdx != null
    val currentCategory = selectedCatIdx?.let { categories.getOrNull(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (showingCategoryItems) viewModel.selectCategory(null)
                else onBack()
            }) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Text(
                if (showingCategoryItems) currentCategory?.name ?: "Add Condition" else "Add Condition",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp, 8.dp)
        ) {
            if (!showingCategoryItems) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(categories) { idx, cat ->
                        CategoryCard(
                            category = cat,
                            onClick = { viewModel.selectCategory(idx) }
                        )
                    }
                }
            } else {
                currentCategory?.let { cat ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(cat.conditions) { idx, template ->
                            ConditionItemRow(
                                template = template,
                                isCustom = cat.name == "Custom",
                                onSelect = {
                                    val cond = ConditionItem(
                                        title = template.title,
                                        hasNum = template.hasNum,
                                        unit = template.unit,
                                        value = if (template.hasNum) null else null
                                    )
                                    val modifyIdx = state.modifyCondIndex
                                    if (modifyIdx != null) {
                                        viewModel.replaceCondition(modifyIdx, cond)
                                    } else {
                                        viewModel.addCondition(cond)
                                    }
                                    viewModel.selectCategory(null)
                                    onConditionSelected()
                                },
                                onManage = if (cat.name == "Custom") {{ viewModel.openManageCustomDialog(idx) }} else null
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateCustom),
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
                    Text("Create Custom Condition", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconForName(category.icon),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (category.name == "Custom" && category.conditions.isNotEmpty()) {
                Text(
                    "${category.conditions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(99.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ConditionItemRow(
    template: ConditionTemplate,
    isCustom: Boolean,
    onSelect: () -> Unit,
    onManage: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(if (isCustom) 0.dp else 12.dp, 12.dp)
            .then(
                if (isCustom) Modifier
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            iconForName(if (isCustom) "tune" else "check_circle_outline"),
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            template.title + if (template.hasNum) " (${template.unit ?: "val"})" else "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (onManage != null) {
            IconButton(onClick = { onManage() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, "Manage")
            }
        }
    }
}
