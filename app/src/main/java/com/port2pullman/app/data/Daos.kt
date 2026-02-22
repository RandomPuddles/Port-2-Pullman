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

    @Query("UPDATE alarms SET enabled = :on, lastStartedAt = CASE WHEN :on = 1 THEN :now ELSE lastStartedAt END WHERE id IN (:ids)")
    suspend fun setEnabled(ids: List<Long>, on: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE alarms SET lastStartedAt = :now WHERE id = :id")
    suspend fun resetLastStartedAt(id: Long, now: Long = System.currentTimeMillis())
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

@Dao
interface TriggerHistoryDao {
    /** Count triggers for a given alarm since [since] epoch-millis. Non-suspend for use from DataSourceResolver. */
    @Query("SELECT COUNT(*) FROM trigger_history WHERE alarmId = :alarmId AND triggeredAt >= :since")
    fun countSince(alarmId: Long, since: Long): Int

    /** Record a new trigger timestamp. */
    @Insert
    suspend fun insert(entry: TriggerHistoryEntity)

    /** Delete all history for an alarm (e.g. when the alarm is deleted). */
    @Query("DELETE FROM trigger_history WHERE alarmId = :alarmId")
    suspend fun deleteForAlarm(alarmId: Long)

    /** Prune entries older than [before] to keep the table small. */
    @Query("DELETE FROM trigger_history WHERE triggeredAt < :before")
    suspend fun pruneBefore(before: Long)
}
