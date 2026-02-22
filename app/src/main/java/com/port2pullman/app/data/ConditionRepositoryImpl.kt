package com.port2pullman.app.data

import com.port2pullman.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConditionRepositoryImpl(
    private val conditionDao: ConditionDao
) : IConditionRepository {

    private val freqAdapter = MoshiProvider.refreshFrequencyAdapter

    /**
     * Built-in condition categories that match the prototype.
     */
    private val builtInCategories: List<Category> = listOf(
        Category(
            name = "Weather", icon = "cloud",
            conditions = listOf(
                LeafCondition("weather", "temperature_above", "Temperature above"),
                LeafCondition("weather", "temperature_below", "Temperature below"),
                LeafCondition("weather", "rain_expected", "Rain expected"),
                LeafCondition("weather", "snow_expected", "Snow expected"),
                LeafCondition("weather", "wind_speed_above", "Wind speed above"),
                LeafCondition("weather", "humidity_above", "Humidity above"),
            )
        ),
        Category(
            name = "Device Attributes", icon = "smartphone",
            conditions = listOf(
                LeafCondition("device", "battery_below", "Battery below"),
                LeafCondition("device", "battery_above", "Battery above"),
                LeafCondition("device", "connected_wifi", "Connected to WiFi"),
                LeafCondition("device", "bluetooth_connected", "Bluetooth connected"),
                LeafCondition("device", "charging", "Charging"),
            )
        ),
        Category(
            name = "Time / Date", icon = "schedule",
            conditions = listOf(
                LeafCondition("time", "time_is", "Time is"),
                LeafCondition("time", "day_of_week", "Day of week is"),
                LeafCondition("time", "date_is", "Date is"),
                LeafCondition("time", "minutes_from_now", "Minutes from now"),
            )
        ),
        Category(
            name = "Location", icon = "location_on",
            conditions = listOf(
                LeafCondition("location", "arrive_at", "Arrive at location"),
                LeafCondition("location", "leave_location", "Leave location"),
                LeafCondition("location", "within_radius", "Within radius of"),
            )
        ),
        Category(
            name = "Recurring Schedule", icon = "event_repeat",
            conditions = listOf(
                LeafCondition("recurring", "every_x_hours", "Every X hours"),
                LeafCondition("recurring", "every_x_days", "Every X days"),
                LeafCondition("recurring", "every_x_weeks", "Every X weeks"),
                LeafCondition("recurring", "x_times_per_day", "X times per day"),
                LeafCondition("recurring", "x_times_per_week", "X times per week"),
            )
        ),
    )

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
            builtInCategories + Category(
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
