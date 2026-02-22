package com.example.eventtriggeralarm.ui

import androidx.lifecycle.ViewModel
import com.example.eventtriggeralarm.data.Alarm
import com.example.eventtriggeralarm.data.ConditionItem
import com.example.eventtriggeralarm.data.RefreshFreq
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppViewModel : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // ─── Home ─────────────────────────────────────────────────
    fun toggleSearch() {
        _state.update { it.copy(searchVisible = !it.searchVisible, searchQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleAlarmEnabled(index: Int) {
        val alarms = _state.value.alarms.toMutableList()
        if (index in alarms.indices) {
            alarms[index] = alarms[index].copy(enabled = !alarms[index].enabled)
            _state.update { it.copy(alarms = alarms) }
        }
    }

    fun enterSelectMode(initialIndex: Int? = null) {
        _state.update {
            it.copy(
                selectMode = true,
                selectedIndices = initialIndex?.let { setOf(it) } ?: emptySet()
            )
        }
    }

    fun exitSelectMode() {
        _state.update { it.copy(selectMode = false, selectedIndices = emptySet()) }
    }

    fun toggleSelection(index: Int) {
        _state.update { state ->
            val newSet = state.selectedIndices.toMutableSet()
            if (index in newSet) newSet.remove(index) else newSet.add(index)
            state.copy(selectedIndices = newSet)
        }
    }

    fun bulkEnable() {
        val indices = _state.value.selectedIndices
        val alarms = _state.value.alarms.toMutableList()
        indices.forEach { if (it in alarms.indices) alarms[it] = alarms[it].copy(enabled = true) }
        _state.update { it.copy(alarms = alarms, selectMode = false, selectedIndices = emptySet()) }
    }

    fun bulkDisable() {
        val indices = _state.value.selectedIndices
        val alarms = _state.value.alarms.toMutableList()
        indices.forEach { if (it in alarms.indices) alarms[it] = alarms[it].copy(enabled = false) }
        _state.update { it.copy(alarms = alarms, selectMode = false, selectedIndices = emptySet()) }
    }

    fun bulkDelete() {
        val indices = _state.value.selectedIndices.sortedDescending()
        val alarms = _state.value.alarms.toMutableList()
        indices.forEach { if (it in alarms.indices) alarms.removeAt(it) }
        _state.update { it.copy(alarms = alarms, selectMode = false, selectedIndices = emptySet()) }
    }

    // ─── Setup ────────────────────────────────────────────────
    fun openCreateAlarm() {
        _state.update {
            it.copy(
                setupTitle = "",
                setupConditions = emptyList(),
                setupOperators = emptyList(),
                setupReadout = false,
                setupRing = false,
                setupTriggerOnce = false,
                setupMode = SetupMode.Create,
                setupAlarmIndex = null,
                modifyCondIndex = null
            )
        }
    }

    fun openEditAlarm(index: Int) {
        val alarm = _state.value.alarms.getOrNull(index) ?: return
        _state.update {
            it.copy(
                setupTitle = alarm.title,
                setupConditions = alarm.conditions,
                setupOperators = alarm.operators,
                setupReadout = alarm.readout,
                setupRing = alarm.ring,
                setupTriggerOnce = alarm.triggerOnce,
                setupMode = SetupMode.Edit,
                setupAlarmIndex = index,
                modifyCondIndex = null
            )
        }
    }

    fun setSetupTitle(title: String) {
        _state.update { it.copy(setupTitle = title) }
    }

    fun setSetupReadout(value: Boolean) {
        _state.update { it.copy(setupReadout = value) }
    }

    fun setSetupRing(value: Boolean) {
        _state.update { it.copy(setupRing = value) }
    }

    fun setSetupTriggerOnce(value: Boolean) {
        _state.update { it.copy(setupTriggerOnce = value) }
    }

    fun addCondition(cond: ConditionItem) {
        _state.update { state ->
            val newConds = state.setupConditions + cond
            val newOps = if (state.setupConditions.isEmpty()) emptyList()
            else state.setupOperators + "AND"
            state.copy(setupConditions = newConds, setupOperators = newOps, modifyCondIndex = null)
        }
    }

    fun replaceCondition(index: Int, cond: ConditionItem) {
        _state.update { state ->
            val newConds = state.setupConditions.toMutableList()
            if (index in newConds.indices) newConds[index] = cond
            state.copy(setupConditions = newConds, modifyCondIndex = null)
        }
    }

    fun removeCondition(index: Int) {
        _state.update { state ->
            val newConds = state.setupConditions.toMutableList()
            val newOps = state.setupOperators.toMutableList()
            if (index in newConds.indices) {
                newConds.removeAt(index)
                when {
                    index == 0 && newOps.isNotEmpty() -> newOps.removeAt(0)
                    index > 0 && index <= newOps.size -> newOps.removeAt(index - 1)
                }
            }
            state.copy(setupConditions = newConds, setupOperators = newOps)
        }
    }

    fun setOperator(index: Int, op: String) {
        _state.update { state ->
            val newOps = state.setupOperators.toMutableList()
            if (index in newOps.indices) newOps[index] = op
            state.copy(setupOperators = newOps, showBoolDialog = false, boolOpIndex = null)
        }
    }

    fun setConditionValue(index: Int, value: Double?) {
        _state.update { state ->
            val newConds = state.setupConditions.toMutableList()
            if (index in newConds.indices) {
                newConds[index] = newConds[index].copy(value = value)
            }
            state.copy(setupConditions = newConds, showNumValDialog = false, numValCondIndex = null)
        }
    }

    fun openAddCondition(modifyIndex: Int? = null) {
        _state.update { it.copy(modifyCondIndex = modifyIndex) }
    }

    fun openBoolDialog(opIndex: Int) {
        _state.update { it.copy(showBoolDialog = true, boolOpIndex = opIndex) }
    }

    fun closeBoolDialog() {
        _state.update { it.copy(showBoolDialog = false, boolOpIndex = null) }
    }

    fun openNumValDialog(condIndex: Int) {
        val cond = _state.value.setupConditions.getOrNull(condIndex)
        _state.update {
            it.copy(
                showNumValDialog = true,
                numValCondIndex = condIndex,
                numValInput = cond?.value?.toString() ?: ""
            )
        }
    }

    fun setNumValInput(value: String) {
        _state.update { it.copy(numValInput = value) }
    }

    fun saveAlarm() {
        val state = _state.value
        val title = state.setupTitle.trim().ifEmpty { "Untitled Alarm" }
        val alarm = Alarm(
            title = title,
            conditions = state.setupConditions,
            operators = state.setupOperators,
            readout = state.setupReadout,
            ring = state.setupRing,
            triggerOnce = state.setupTriggerOnce,
            enabled = true
        )
        when (state.setupMode) {
            SetupMode.Create -> {
                val alarms = state.alarms + alarm
                _state.update { it.copy(alarms = alarms) }
            }
            SetupMode.Edit -> {
                val idx = state.setupAlarmIndex ?: return
                val alarms = state.alarms.toMutableList()
                if (idx in alarms.indices) {
                    alarms[idx] = alarm.copy(enabled = alarms[idx].enabled)
                    _state.update { it.copy(alarms = alarms) }
                }
            }
        }
    }

    fun deleteAlarm(index: Int) {
        val alarms = _state.value.alarms.toMutableList()
        if (index in alarms.indices) {
            alarms.removeAt(index)
            _state.update { it.copy(alarms = alarms) }
        }
    }

    fun showConfirmDeleteAlarm(index: Int) {
        val alarm = _state.value.alarms.getOrNull(index) ?: return
        _state.update {
            it.copy(
                showConfirmDelete = true,
                confirmDeleteTitle = "Delete alarm?",
                confirmDeleteMessage = "\"${alarm.title}\" will be permanently deleted.",
                pendingDeleteAction = { deleteAlarm(index) }
            )
        }
    }

    fun showConfirmDeleteBulk() {
        val count = _state.value.selectedIndices.size
        _state.update {
            it.copy(
                showConfirmDelete = true,
                confirmDeleteTitle = "Delete $count alarm${if (count > 1) "s" else ""}?",
                confirmDeleteMessage = "$count alarm${if (count > 1) "s" else ""} will be permanently deleted.",
                pendingDeleteAction = { bulkDelete() }
            )
        }
    }

    fun cancelConfirmDelete() {
        _state.update {
            it.copy(showConfirmDelete = false, pendingDeleteAction = null)
        }
    }

    fun executePendingDelete() {
        _state.value.pendingDeleteAction?.invoke()
        _state.update { it.copy(showConfirmDelete = false, pendingDeleteAction = null) }
    }

    // ─── Custom conditions ─────────────────────────────────────
    fun openCreateCustomCond() {
        _state.update {
            it.copy(
                showCustomCondDialog = true,
                customCondTitle = "",
                customCondStmt = "",
                customCondFreqVal = "",
                customCondFreqUnit = "minutes",
                manageCustomIndex = null
            )
        }
    }

    fun openModifyCustomCond(index: Int) {
        val cond = _state.value.customConditions.getOrNull(index) ?: return
        val (title, stmt) = when {
            cond.title.contains(": ") -> {
                val idx = cond.title.indexOf(": ")
                cond.title.substring(0, idx) to cond.title.substring(idx + 2)
            }
            else -> cond.title to ""
        }
        val freq = cond.refreshFreq
        _state.update {
            it.copy(
                showCustomCondDialog = true,
                customCondTitle = title,
                customCondStmt = stmt,
                customCondFreqVal = freq?.value?.toString() ?: "",
                customCondFreqUnit = freq?.unit ?: "minutes",
                manageCustomIndex = index
            )
        }
    }

    fun setCustomCondTitle(value: String) {
        _state.update { it.copy(customCondTitle = value) }
    }

    fun setCustomCondStmt(value: String) {
        _state.update { it.copy(customCondStmt = value) }
    }

    fun setCustomCondFreqVal(value: String) {
        _state.update { it.copy(customCondFreqVal = value) }
    }

    fun setCustomCondFreqUnit(value: String) {
        _state.update { it.copy(customCondFreqUnit = value) }
    }

    fun saveCustomCondition() {
        val state = _state.value
        val title = state.customCondTitle.trim()
        if (title.isEmpty()) return
        val fullTitle = if (state.customCondStmt.isNotBlank()) "$title: ${state.customCondStmt.trim()}" else title
        val freqVal = state.customCondFreqVal.toIntOrNull()
        val freq = if (freqVal != null && freqVal > 0)
            RefreshFreq(freqVal, state.customCondFreqUnit) else null
        val cond = ConditionItem(
            title = fullTitle,
            hasNum = false,
            custom = true,
            refreshFreq = freq
        )
        val customConds = state.customConditions.toMutableList()
        state.manageCustomIndex?.let { idx ->
            if (idx in customConds.indices) customConds[idx] = cond
        } ?: run { customConds.add(cond) }
        _state.update {
            it.copy(
                customConditions = customConds,
                showCustomCondDialog = false,
                manageCustomIndex = null
            )
        }
    }

    fun deleteCustomCondition(index: Int) {
        val customConds = _state.value.customConditions.toMutableList()
        if (index in customConds.indices) {
            customConds.removeAt(index)
            _state.update { it.copy(customConditions = customConds) }
        }
    }

    fun closeCustomCondDialog() {
        _state.update { it.copy(showCustomCondDialog = false, manageCustomIndex = null) }
    }

    fun selectCategory(index: Int?) {
        _state.update { it.copy(selectedCategoryIndex = index) }
    }

    fun openManageCustomDialog(index: Int) {
        _state.update { it.copy(showManageCustomDialog = true, manageCustomIndex = index) }
    }

    fun closeManageCustomDialog() {
        _state.update { it.copy(showManageCustomDialog = false, manageCustomIndex = null) }
    }

    // ─── AI Dialog ────────────────────────────────────────────
    fun openAiDialog() {
        _state.update { it.copy(showAiDialog = true, aiPrompt = "") }
    }

    fun setAiPrompt(value: String) {
        _state.update { it.copy(aiPrompt = value) }
    }

    fun closeAiDialog() {
        _state.update { it.copy(showAiDialog = false, aiPrompt = "") }
    }

    fun generateAlarmFromAi() {
        val prompt = _state.value.aiPrompt.trim()
        if (prompt.isEmpty()) return
        // Simulate AI generation (keyword-based like prototype)
        val lower = prompt.lowercase()
        val conditions = mutableListOf<ConditionItem>()
        if (lower.contains("rain")) conditions.add(ConditionItem("Rain expected", false))
        if (lower.contains("cold") || lower.contains("freez")) {
            conditions.add(ConditionItem("Temperature below", true, "°F", 32.0))
        }
        if (lower.contains("hot") || lower.contains("heat")) {
            conditions.add(ConditionItem("Temperature above", true, "°F", 90.0))
        }
        if (lower.contains("battery") || lower.contains("charge")) {
            conditions.add(ConditionItem("Battery below", true, "%", 20.0))
        }
        if (lower.contains("morning")) conditions.add(ConditionItem("Time is", false))
        if (lower.contains("location") || lower.contains("arrive") || lower.contains("home") || lower.contains("work")) {
            conditions.add(ConditionItem("Arrive at location", false))
        }
        if (lower.contains("every") && lower.contains("hour")) {
            conditions.add(ConditionItem("Every X hours", true, "hrs", 1.0))
        }
        if (lower.contains("wind")) {
            conditions.add(ConditionItem("Wind speed above", true, "mph", 25.0))
        }
        if (conditions.isEmpty()) conditions.add(ConditionItem("Time is", false))
        val operators = List(conditions.size - 1) { "AND" }
        val title = if (prompt.length > 40) prompt.take(40) + "…" else prompt
        _state.update {
            it.copy(
                showAiDialog = false,
                aiPrompt = "",
                setupTitle = title,
                setupConditions = conditions,
                setupOperators = operators,
                setupReadout = lower.contains("read") || lower.contains("speak") || lower.contains("say"),
                setupRing = lower.contains("ring") || lower.contains("alarm") || lower.contains("loud"),
                setupTriggerOnce = lower.contains("once") || lower.contains("one time"),
                setupMode = SetupMode.Create,
                setupAlarmIndex = null
            )
        }
    }

    // ─── Dialogs ─────────────────────────────────────────────
    fun closeNumValDialog() {
        _state.update { it.copy(showNumValDialog = false, numValCondIndex = null) }
    }

    fun showTriggeredDemo(alarm: Alarm) {
        val (icon, msg) = when {
            alarm.ring && alarm.readout -> "alarm" to "🔔 Ringing… (after dismiss, readout will play)"
            alarm.ring -> "alarm" to "🔔 Alarm ringing until dismissed"
            alarm.readout -> "record_voice_over" to "🗣️ Reading title aloud until dismissed"
            else -> "notifications" to "📌 ${alarm.title}"
        }
        _state.update {
            it.copy(
                showTriggeredDialog = true,
                triggeredAlarm = alarm,
                triggeredIcon = icon,
                triggeredMessage = msg
            )
        }
        if (alarm.triggerOnce) {
            val idx = _state.value.alarms.indexOf(alarm)
            if (idx >= 0) toggleAlarmEnabled(idx)
        }
    }

    fun dismissTriggered() {
        _state.update {
            it.copy(showTriggeredDialog = false, triggeredAlarm = null)
        }
    }
}
