package com.port2pullman.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val conditionTreeJson: String,
    val readout: Boolean = false,
    val ring: Boolean = false,
    val triggerOnce: Boolean = false,
    val enabled: Boolean = true,
    val lastStartedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_conditions")
data class CustomConditionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val statement: String,
    val refreshFreqJson: String
)

@Entity(tableName = "trigger_history")
data class TriggerHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val triggeredAt: Long = System.currentTimeMillis()
)
