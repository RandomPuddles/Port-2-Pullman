package com.port2pullman.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.port2pullman.app.data.IAlarmRepository
import com.port2pullman.app.data.IConditionRepository
import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Flat editing state for Setup & AddCondition screens.
 * Mirrors the prototype's `editState` closely.
 */
data class SetupUiState(
    val title: String = "",
    val conditions: List<LeafCondition> = emptyList(),
    val operators: List<Operator> = emptyList(),
    val readout: Boolean = false,
    val ring: Boolean = false,
    val triggerOnce: Boolean = false,
    val isEditing: Boolean = false,
    val alarmId: Long? = null,
    // Condition browser
    val categories: List<Category> = emptyList(),
    val selectedCategoryIndex: Int? = null,
    val modifyConditionIndex: Int? = null,
    // Numerical value popup
    val showNumValPopup: Boolean = false,
    val numValCondIndex: Int = -1,
    // Bool operator popup
    val showBoolPopup: Boolean = false,
    val boolOpIndex: Int = -1,
    // Delete alarm confirmation
    val showDeleteConfirm: Boolean = false,
    // Custom condition popup
    val showCustomCondPopup: Boolean = false,
    val customCondEditIndex: Int? = null,
    // Manage custom popup
    val showManageCustomPopup: Boolean = false,
    val manageCustomIndex: Int? = null,
    val manageCustomTitle: String = "",
)

class SetupViewModel(
    private val alarmRepo: IAlarmRepository,
    private val conditionRepo: IConditionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _state.asStateFlow()

    /** Prevents re-initialization when returning from AddConditionScreen. */
    private var initialized = false

    init {
        DebugLog.d("SetupVM", "Created instance #${System.identityHashCode(this)}")
        // Observe categories
        viewModelScope.launch {
            conditionRepo.getCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
    }

    // ─── Init for create / edit ──────────────────────────────
    fun initForCreate() {
        if (initialized) {
            DebugLog.d("SetupVM", "initForCreate() SKIPPED (already initialized, ${_state.value.conditions.size} conditions)")
            return
        }
        initialized = true
        DebugLog.d("SetupVM", "initForCreate() running — resetting state")
        _state.value = _state.value.copy(
            title = "",
            conditions = emptyList(),
            operators = emptyList(),
            readout = false,
            ring = false,
            triggerOnce = false,
            isEditing = false,
            alarmId = null,
            selectedCategoryIndex = null,
        )
    }

    fun initForEdit(alarmId: Long) {
        if (initialized) {
            DebugLog.d("SetupVM", "initForEdit($alarmId) SKIPPED (already initialized)")
            return
        }
        initialized = true
        DebugLog.d("SetupVM", "initForEdit($alarmId) running — loading alarm")
        viewModelScope.launch {
            alarmRepo.getById(alarmId)
                .filterNotNull()
                .first()
                .let { alarm ->
                    val (leaves, ops) = flattenCondition(alarm.rootCondition)
                    _state.update {
                        it.copy(
                            title = alarm.title,
                            conditions = leaves,
                            operators = ops,
                            readout = alarm.readout,
                            ring = alarm.ring,
                            triggerOnce = alarm.triggerOnce,
                            isEditing = true,
                            alarmId = alarm.id,
                        )
                    }
                }
        }
    }

    /** Populate from an AI-generated draft. */
    fun applyDraft(draft: AlarmDraft) {
        val (leaves, ops) = flattenCondition(draft.rootCondition)
        _state.update {
            it.copy(
                title = draft.title,
                conditions = leaves,
                operators = ops,
                readout = draft.readout,
                ring = draft.ring,
                triggerOnce = draft.triggerOnce,
            )
        }
    }

    // ─── Title ───────────────────────────────────────────────
    fun setTitle(t: String) = _state.update { it.copy(title = t) }

    // ─── Conditions ──────────────────────────────────────────
    fun openAddCondition() = _state.update { it.copy(modifyConditionIndex = null) }
    fun openModifyCondition(index: Int) = _state.update { it.copy(modifyConditionIndex = index) }

    fun selectCondition(condition: LeafCondition) {
        DebugLog.i("SetupVM", "selectCondition('${condition.label}') on VM #${System.identityHashCode(this)}")
        _state.update { s ->
            val modIdx = s.modifyConditionIndex
            if (modIdx != null && modIdx in s.conditions.indices) {
                // Replace existing
                val newConds = s.conditions.toMutableList()
                newConds[modIdx] = condition
                DebugLog.d("SetupVM", "Replaced condition at index $modIdx → ${newConds.size} total")
                s.copy(conditions = newConds, modifyConditionIndex = null)
            } else {
                // Add new
                val newOps = if (s.conditions.isNotEmpty())
                    s.operators + Operator.AND else s.operators
                DebugLog.d("SetupVM", "Added condition → ${s.conditions.size + 1} total")
                s.copy(
                    conditions = s.conditions + condition,
                    operators = newOps,
                    modifyConditionIndex = null,
                )
            }
        }
    }

    fun removeCondition(index: Int) {
        DebugLog.d("SetupVM", "removeCondition($index)")
        _state.update { s ->
            val newConds = s.conditions.toMutableList().apply { removeAt(index) }
            val newOps = s.operators.toMutableList()
            if (index == 0 && newOps.isNotEmpty()) newOps.removeAt(0)
            else if (index > 0 && newOps.size >= index) newOps.removeAt(index - 1)
            s.copy(conditions = newConds, operators = newOps)
        }
    }

    fun reorderCondition(from: Int, to: Int) {
        _state.update { s ->
            val newConds = s.conditions.toMutableList()
            val item = newConds.removeAt(from)
            newConds.add(to, item)
            // Keep operators length consistent
            val newOps = s.operators.toMutableList()
            while (newOps.size < newConds.size - 1) newOps.add(Operator.AND)
            while (newOps.size > maxOf(0, newConds.size - 1)) newOps.removeLast()
            s.copy(conditions = newConds, operators = newOps)
        }
    }

    fun toggleNegation(index: Int) {
        _state.update { s ->
            if (index !in s.conditions.indices) return@update s
            val newConds = s.conditions.toMutableList()
            newConds[index] = newConds[index].let { it.copy(negated = !it.negated) }
            DebugLog.d("SetupVM", "toggleNegation($index) → negated=${newConds[index].negated}")
            s.copy(conditions = newConds)
        }
    }

    // ─── Numerical Value ─────────────────────────────────────
    fun openNumVal(condIndex: Int) =
        _state.update { it.copy(showNumValPopup = true, numValCondIndex = condIndex) }

    fun closeNumVal() =
        _state.update { it.copy(showNumValPopup = false) }

    fun setNumVal(value: Double) {
        _state.update { s ->
            val idx = s.numValCondIndex
            if (idx in s.conditions.indices) {
                val newConds = s.conditions.toMutableList()
                newConds[idx] = newConds[idx].copy(value = value)
                s.copy(conditions = newConds, showNumValPopup = false)
            } else s
        }
    }

    // ─── Boolean Operator ────────────────────────────────────
    fun openBoolPopup(opIndex: Int) =
        _state.update { it.copy(showBoolPopup = true, boolOpIndex = opIndex) }

    fun closeBoolPopup() =
        _state.update { it.copy(showBoolPopup = false) }

    fun setBoolOp(op: Operator) {
        _state.update { s ->
            val idx = s.boolOpIndex
            if (idx in s.operators.indices) {
                val newOps = s.operators.toMutableList()
                newOps[idx] = op
                s.copy(operators = newOps, showBoolPopup = false)
            } else s.copy(showBoolPopup = false)
        }
    }

    // ─── Options ─────────────────────────────────────────────
    fun toggleReadout() = _state.update { it.copy(readout = !it.readout) }
    fun toggleRing() = _state.update { it.copy(ring = !it.ring) }
    fun toggleTriggerOnce() = _state.update { it.copy(triggerOnce = !it.triggerOnce) }

    // ─── Delete Alarm ────────────────────────────────────────
    fun requestDeleteAlarm() = _state.update { it.copy(showDeleteConfirm = true) }
    fun cancelDeleteAlarm() = _state.update { it.copy(showDeleteConfirm = false) }
    fun confirmDeleteAlarm(onDone: () -> Unit) {
        val id = _state.value.alarmId ?: return
        viewModelScope.launch {
            alarmRepo.delete(listOf(id))
            _state.update { it.copy(showDeleteConfirm = false) }
            onDone()
        }
    }

    // ─── Save ────────────────────────────────────────────────
    fun save(onDone: () -> Unit) {
        val s = _state.value
        DebugLog.i("SetupVM", "save() — title='${s.title}', ${s.conditions.size} conditions")
        val title = s.title.ifBlank { "Untitled Alarm" }
        val root = buildComposite(s.conditions, s.operators)
        val alarm = Alarm(
            id = s.alarmId ?: 0,
            title = title,
            rootCondition = root,
            readout = s.readout,
            ring = s.ring,
            triggerOnce = s.triggerOnce,
            enabled = true,
        )
        viewModelScope.launch {
            alarmRepo.upsert(alarm)
            onDone()
        }
    }

    // ─── Category Browser ────────────────────────────────────
    fun selectCategory(index: Int) =
        _state.update { it.copy(selectedCategoryIndex = index) }

    fun clearCategory() =
        _state.update { it.copy(selectedCategoryIndex = null) }

    // ─── Custom Condition CRUD ───────────────────────────────
    fun openCreateCustom() =
        _state.update { it.copy(showCustomCondPopup = true, customCondEditIndex = null) }

    fun openModifyCustom(condIndex: Int) {
        _state.update { it.copy(showCustomCondPopup = true, customCondEditIndex = condIndex, showManageCustomPopup = false) }
    }

    fun closeCustomCondPopup() =
        _state.update { it.copy(showCustomCondPopup = false) }

    fun saveCustomCondition(title: String, statement: String, freqValue: Int, freqUnit: TimeUnit) {
        val s = _state.value
        val catIdx = s.categories.indexOfFirst { it.name == "Custom" }
        val existingId = if (s.customCondEditIndex != null && catIdx >= 0) {
            // We don't have the actual DB id here; use 0 for new
            0L
        } else 0L

        val cond = CustomCondition(
            id = existingId,
            title = title,
            statement = statement,
            refreshFrequency = RefreshFrequency(freqValue, freqUnit)
        )
        viewModelScope.launch {
            conditionRepo.upsertCustom(cond)
        }
        _state.update { it.copy(showCustomCondPopup = false) }
    }

    fun openManageCustom(condIndex: Int, title: String) =
        _state.update { it.copy(showManageCustomPopup = true, manageCustomIndex = condIndex, manageCustomTitle = title) }

    fun closeManageCustom() =
        _state.update { it.copy(showManageCustomPopup = false) }

    fun deleteCustomCondition() {
        val s = _state.value
        val idx = s.manageCustomIndex ?: return
        // The custom conditions are the last category
        val customCat = s.categories.lastOrNull { it.name == "Custom" }
        if (customCat != null && idx in customCat.conditions.indices) {
            val type = customCat.conditions[idx].type
            val idStr = type.removePrefix("custom_")
            val id = idStr.toLongOrNull() ?: return
            viewModelScope.launch { conditionRepo.deleteCustom(id) }
        }
        _state.update { it.copy(showManageCustomPopup = false) }
    }

    // ─── Tree ⟷ Flat conversion ─────────────────────────────
    companion object {
        fun flattenCondition(condition: Condition): Pair<List<LeafCondition>, List<Operator>> {
            val leaves = mutableListOf<LeafCondition>()
            val ops = mutableListOf<Operator>()
            flattenRecursive(condition, leaves, ops, null)
            return leaves to ops
        }

        private fun flattenRecursive(
            cond: Condition,
            leaves: MutableList<LeafCondition>,
            ops: MutableList<Operator>,
            parentOp: Operator?
        ) {
            when (cond) {
                is LeafCondition -> {
                    if (leaves.isNotEmpty() && parentOp != null) ops.add(parentOp)
                    leaves.add(cond)
                }
                is CompositeCondition -> {
                    for ((i, child) in cond.children.withIndex()) {
                        if (i > 0 && child is LeafCondition && leaves.isNotEmpty()) {
                            ops.add(cond.operator)
                        }
                        flattenRecursive(child, leaves, ops, cond.operator)
                    }
                }
            }
        }

        fun buildComposite(
            conditions: List<LeafCondition>,
            operators: List<Operator>
        ): CompositeCondition {
            if (conditions.isEmpty()) return CompositeCondition(Operator.AND)
            if (conditions.size == 1) return CompositeCondition(Operator.AND, conditions)
            val op = operators.firstOrNull() ?: Operator.AND
            return CompositeCondition(op, conditions)
        }
    }
}
