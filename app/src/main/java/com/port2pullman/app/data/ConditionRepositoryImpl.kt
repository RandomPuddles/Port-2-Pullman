package com.port2pullman.app.data

import com.port2pullman.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConditionRepositoryImpl(
    private val conditionDao: ConditionDao
) : IConditionRepository {

    private val freqAdapter = MoshiProvider.refreshFrequencyAdapter

    override fun getCategories(): Flow<List<Category>> {
        return conditionDao.getAll().map { customEntities ->
            val customConditions = customEntities.map { toDomain(it) }
            val customLeafs = customConditions.map { cc ->
                LeafCondition(
                    category = "custom",
                    type = "custom_${cc.id}",
                    label = if (cc.statement.isNotBlank()) "${cc.title}: ${cc.statement}" else cc.title
                )
            }
            ConditionRegistry.categories + Category(
                name = "Custom",
                icon = "tune",
                conditions = customLeafs
            )
        }
    }

    override suspend fun upsertCustom(condition: CustomCondition) {
        conditionDao.upsert(toEntity(condition))
    }

    override suspend fun deleteCustom(id: Long) {
        conditionDao.deleteById(id)
    }

    private fun toEntity(c: CustomCondition) = CustomConditionEntity(
        id = c.id,
        title = c.title,
        statement = c.statement,
        refreshFreqJson = freqAdapter.toJson(c.refreshFrequency)
    )

    private fun toDomain(e: CustomConditionEntity) = CustomCondition(
        id = e.id,
        title = e.title,
        statement = e.statement,
        refreshFrequency = freqAdapter.fromJson(e.refreshFreqJson)
    )
}
