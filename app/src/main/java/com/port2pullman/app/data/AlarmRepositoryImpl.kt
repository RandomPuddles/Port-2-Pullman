package com.port2pullman.app.data

import com.port2pullman.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepositoryImpl(
    private val alarmDao: AlarmDao
) : IAlarmRepository {

    private val treeAdapter = MoshiProvider.conditionTreeAdapter

    override fun getAll(): Flow<List<Alarm>> =
        alarmDao.getAll().map { entities -> entities.map { toDomain(it) } }

    override fun search(query: String): Flow<List<Alarm>> =
        alarmDao.search(query).map { entities -> entities.map { toDomain(it) } }

    override fun getById(id: Long): Flow<Alarm?> =
        alarmDao.getById(id).map { entity -> entity?.let { toDomain(it) } }

    override suspend fun upsert(alarm: Alarm): Long =
        alarmDao.upsert(toEntity(alarm))

    override suspend fun delete(ids: List<Long>) =
        alarmDao.deleteByIds(ids)

    override suspend fun setEnabled(ids: List<Long>, on: Boolean) =
        alarmDao.setEnabled(ids, on)

    private fun toEntity(alarm: Alarm) = AlarmEntity(
        id = alarm.id,
        title = alarm.title,
        conditionTreeJson = treeAdapter.toJson(alarm.rootCondition),
        readout = alarm.readout,
        ring = alarm.ring,
        triggerOnce = alarm.triggerOnce,
        enabled = alarm.enabled,
        createdAt = alarm.createdAt
    )

    private fun toDomain(entity: AlarmEntity) = Alarm(
        id = entity.id,
        title = entity.title,
        rootCondition = treeAdapter.fromJson(entity.conditionTreeJson),
        readout = entity.readout,
        ring = entity.ring,
        triggerOnce = entity.triggerOnce,
        enabled = entity.enabled,
        createdAt = entity.createdAt
    )
}
