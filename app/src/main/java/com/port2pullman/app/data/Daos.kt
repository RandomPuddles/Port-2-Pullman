package com.port2pullman.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY id DESC")
    fun getAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE title LIKE '%' || :query || '%' OR conditionTreeJson LIKE '%' || :query || '%' ORDER BY id DESC")
    fun search(query: String): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getById(id: Long): Flow<AlarmEntity?>

    @Upsert
    suspend fun upsert(entity: AlarmEntity): Long

    @Query("DELETE FROM alarms WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE alarms SET enabled = :on WHERE id IN (:ids)")
    suspend fun setEnabled(ids: List<Long>, on: Boolean)
}

@Dao
interface ConditionDao {
    @Query("SELECT * FROM custom_conditions ORDER BY id ASC")
    fun getAll(): Flow<List<CustomConditionEntity>>

    @Upsert
    suspend fun upsert(entity: CustomConditionEntity)

    @Query("DELETE FROM custom_conditions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
