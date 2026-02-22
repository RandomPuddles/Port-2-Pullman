package com.port2pullman.app.data

import com.port2pullman.app.model.Alarm
import com.port2pullman.app.model.Category
import com.port2pullman.app.model.CustomCondition
import kotlinx.coroutines.flow.Flow

/**
 * Cross-package contract for alarm persistence.
 */
interface IAlarmRepository {
    fun getAll(): Flow<List<Alarm>>
    fun search(query: String): Flow<List<Alarm>>
    fun getById(id: Long): Flow<Alarm?>
    suspend fun upsert(alarm: Alarm): Long
    suspend fun delete(ids: List<Long>)
    suspend fun setEnabled(ids: List<Long>, on: Boolean)
}

/**
 * Cross-package contract for condition categories.
 */
interface IConditionRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun upsertCustom(condition: CustomCondition)
    suspend fun deleteCustom(id: Long)
}
