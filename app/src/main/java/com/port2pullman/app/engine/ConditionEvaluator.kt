package com.port2pullman.app.engine

import com.port2pullman.app.model.*

/**
 * Strategy interface for evaluating individual condition types.
 */
interface ConditionEvaluator {
    /** The set of condition types this evaluator handles. */
    val supportedTypes: Set<String>

    /**
     * Evaluate a single [LeafCondition] and return true if it is satisfied.
     */
    suspend fun evaluate(condition: LeafCondition): Boolean
}

/** Evaluates weather-related conditions (stub – needs real API integration). */
class WeatherEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "temperature_above", "temperature_below",
        "rain_expected", "snow_expected",
        "wind_speed_above", "humidity_above"
    )

    override suspend fun evaluate(condition: LeafCondition): Boolean {
        // TODO: Integrate with a weather API (OpenWeatherMap, etc.)
        return false
    }
}

/** Evaluates device attribute conditions. */
class DeviceEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "battery_below", "battery_above",
        "connected_wifi", "bluetooth_connected", "charging"
    )

    override suspend fun evaluate(condition: LeafCondition): Boolean {
        // TODO: Read real device state via BatteryManager, ConnectivityManager, etc.
        return false
    }
}

/** Evaluates time and date conditions. */
class TimeEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "time_is", "day_of_week", "date_is", "minutes_from_now"
    )

    override suspend fun evaluate(condition: LeafCondition): Boolean {
        // TODO: Compare against system clock
        return false
    }
}

/** Evaluates location-based conditions. */
class LocationEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "arrive_at", "leave_location", "within_radius"
    )

    override suspend fun evaluate(condition: LeafCondition): Boolean {
        // TODO: Integrate with FusedLocationProviderClient
        return false
    }
}

/** Evaluates recurring schedule conditions. */
class RecurringEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "every_x_hours", "every_x_days", "every_x_weeks",
        "x_times_per_day", "x_times_per_week"
    )

    override suspend fun evaluate(condition: LeafCondition): Boolean {
        // TODO: Track last trigger time and compare intervals
        return false
    }
}

/** Evaluates user-defined custom conditions. */
class CustomEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf<String>() // matches anything starting with "custom_"

    override suspend fun evaluate(condition: LeafCondition): Boolean {
        // TODO: Parse custom condition statement, evaluate with Gemini or rule engine
        return false
    }

    fun canHandle(type: String): Boolean = type.startsWith("custom_")
}

/**
 * Composite evaluator that delegates to the appropriate strategy
 * and applies AND/OR logic for composite conditions.
 */
class ConditionTreeEvaluator(
    private val evaluators: List<ConditionEvaluator> = listOf(
        WeatherEvaluator(),
        DeviceEvaluator(),
        TimeEvaluator(),
        LocationEvaluator(),
        RecurringEvaluator(),
    ),
    private val customEvaluator: CustomEvaluator = CustomEvaluator()
) {
    suspend fun evaluate(condition: Condition): Boolean = when (condition) {
        is LeafCondition -> evaluateLeaf(condition)
        is CompositeCondition -> evaluateComposite(condition)
    }

    private suspend fun evaluateLeaf(leaf: LeafCondition): Boolean {
        if (customEvaluator.canHandle(leaf.type)) {
            return customEvaluator.evaluate(leaf)
        }
        val evaluator = evaluators.firstOrNull { leaf.type in it.supportedTypes }
        return evaluator?.evaluate(leaf) ?: false
    }

    private suspend fun evaluateComposite(composite: CompositeCondition): Boolean {
        if (composite.children.isEmpty()) return false
        return when (composite.operator) {
            Operator.AND -> composite.children.all { evaluate(it) }
            Operator.OR -> composite.children.any { evaluate(it) }
        }
    }
}
