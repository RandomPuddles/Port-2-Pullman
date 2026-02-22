package com.port2pullman.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.port2pullman.app.data.IAlarmRepository
import com.port2pullman.app.model.Alarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val alarms: List<Alarm> = emptyList(),
    val searchQuery: String = "",
    val searchVisible: Boolean = false,
    val selectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val showDeleteConfirm: Boolean = false,
    val deleteConfirmTitle: String = "",
    val deleteConfirmMsg: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmListViewModel(
    private val repo: IAlarmRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiFlags = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repo.getAll() else repo.search(query)
        }
        .combine(_uiFlags) { alarms, flags ->
            flags.copy(alarms = alarms, searchQuery = _searchQuery.value)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // ─── Search ──────────────────────────────────────────────
    fun showSearch() = updateFlags { copy(searchVisible = true) }
    fun hideSearch() {
        _searchQuery.value = ""
        updateFlags { copy(searchVisible = false) }
    }
    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    // ─── Toggle Enable/Disable ───────────────────────────────
    fun toggleEnabled(alarm: Alarm) {
        viewModelScope.launch {
            repo.setEnabled(listOf(alarm.id), !alarm.enabled)
        }
    }

    // ─── Select Mode ─────────────────────────────────────────
    fun enterSelectMode(alarmId: Long) {
        updateFlags {
            copy(selectMode = true, selectedIds = setOf(alarmId))
        }
    }

    fun exitSelectMode() {
        updateFlags {
            copy(selectMode = false, selectedIds = emptySet())
        }
    }

    fun toggleSelection(alarmId: Long) {
        updateFlags {
            val newSet = selectedIds.toMutableSet()
            if (alarmId in newSet) newSet.remove(alarmId) else newSet.add(alarmId)
            copy(selectedIds = newSet)
        }
    }

    // ─── Bulk Operations ─────────────────────────────────────
    fun bulkEnable() {
        val ids = _uiFlags.value.selectedIds.toList()
        viewModelScope.launch {
            repo.setEnabled(ids, true)
        }
        exitSelectMode()
    }

    fun bulkDisable() {
        val ids = _uiFlags.value.selectedIds.toList()
        viewModelScope.launch {
            repo.setEnabled(ids, false)
        }
        exitSelectMode()
    }

    fun requestBulkDelete() {
        val count = _uiFlags.value.selectedIds.size
        if (count == 0) return
        updateFlags {
            copy(
                showDeleteConfirm = true,
                deleteConfirmTitle = "Delete $count alarm${if (count > 1) "s" else ""}?",
                deleteConfirmMsg = "$count alarm${if (count > 1) "s" else ""} will be permanently deleted."
            )
        }
    }

    fun confirmDelete() {
        val ids = _uiFlags.value.selectedIds.toList()
        viewModelScope.launch {
            repo.delete(ids)
        }
        updateFlags { copy(showDeleteConfirm = false) }
        exitSelectMode()
    }

    fun cancelDelete() {
        updateFlags { copy(showDeleteConfirm = false) }
    }

    private fun updateFlags(block: HomeUiState.() -> HomeUiState) {
        _uiFlags.update { it.block() }
    }
}
