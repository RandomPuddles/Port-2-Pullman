package com.example.eventtriggeralarm.ui

import com.example.eventtriggeralarm.data.Alarm
import com.example.eventtriggeralarm.data.ConditionItem
import com.example.eventtriggeralarm.data.RefreshFreq

data class AppState(
    val alarms: List<Alarm> = emptyList(),
    val searchQuery: String = "",
    val searchVisible: Boolean = false,
    val selectMode: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    // Setup screen state
    val setupTitle: String = "",
    val setupConditions: List<ConditionItem> = emptyList(),
    val setupOperators: List<String> = emptyList(),
    val setupReadout: Boolean = false,
    val setupRing: Boolean = false,
    val setupTriggerOnce: Boolean = false,
    val setupMode: SetupMode = SetupMode.Create,
    val setupAlarmIndex: Int? = null,
    val modifyCondIndex: Int? = null,
    // Custom conditions (persisted)
    val customConditions: List<ConditionItem> = emptyList(),
    // Dialogs
    val showBoolDialog: Boolean = false,
    val boolOpIndex: Int? = null,
    val showAiDialog: Boolean = false,
    val aiPrompt: String = "",
    val showCustomCondDialog: Boolean = false,
    val customCondTitle: String = "",
    val customCondStmt: String = "",
    val customCondFreqVal: String = "",
    val customCondFreqUnit: String = "minutes",
    val manageCustomIndex: Int? = null,
    val selectedCategoryIndex: Int? = null,
    val showNumValDialog: Boolean = false,
    val numValCondIndex: Int? = null,
    val numValInput: String = "",
    val showConfirmDelete: Boolean = false,
    val confirmDeleteTitle: String = "",
    val confirmDeleteMessage: String = "",
    val pendingDeleteAction: (() -> Unit)? = null,
    val showManageCustomDialog: Boolean = false,
    val showTriggeredDialog: Boolean = false,
    val triggeredAlarm: Alarm? = null,
    val triggeredIcon: String = "notifications_active",
    val triggeredMessage: String = ""
)

enum class SetupMode { Create, Edit }
