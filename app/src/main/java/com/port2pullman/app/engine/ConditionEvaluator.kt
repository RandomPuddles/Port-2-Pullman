package com.port2pullman.app.engine

import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.model.*
import java.util.Calendar

/**
 * Strategy interface for evaluating individual condition types.
 */
interface ConditionEvaluator {
    /** The set of condition types this evaluator handles. */
    val supportedTypes: Set<String>

    /**
     * Evaluate a single [LeafCondition] and return true if it is satisfied.
     * [alarmStartedAt] is the epoch-millis when the alarm was last started/enabled.
     */
    suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean
}

/** Evaluates weather-related conditions (stub – needs real API integration). */
class WeatherEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "temperature_above", "temperature_below",
        "rain_expected", "snow_expected",
        "wind_speed_above", "humidity_above"
    )

    override suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        DebugLog.w("WeatherEval", "${condition.type}: stub — always false (needs API key)")
        return false
    }
}

/** Evaluates device attribute conditions. */
class DeviceEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "battery_below", "battery_above",
        "connected_wifi", "bluetooth_connected", "charging"
    )

    override suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        DebugLog.w("DeviceEval", "${condition.type}: stub — always false (needs system APIs)")
        return false
    }
}

/** Evaluates time and date conditions — fully implemented. */
class TimeEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "time_is", "day_of_week", "date_is", "minutes_from_now"
    )

    override suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        return when (condition.type) {
            "minutes_from_now" -> evaluateMinutesFromNow(condition, alarmStartedAt)
            "time_is" -> evaluateTimeIs(condition)
            "day_of_week" -> evaluateDayOfWeek(condition)
            else -> {
                DebugLog.w("TimeEval", "${condition.type}: not yet implemented")
                false
            }
        }
    }

    private fun evaluateMinutesFromNow(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        val minutes = when (val v = condition.value) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val targetTime = alarmStartedAt + (minutes * 60_000L).toLong()
        val now = System.currentTimeMillis()
        val remainingMs = targetTime - now
        val triggered = now >= targetTime

        DebugLog.d(
            "TimeEval",
            "minutes_from_now: value=${minutes}m, startedAt=${alarmStartedAt}, " +
                    "target=$targetTime, now=$now, remaining=${remainingMs / 1000}s, triggered=$triggered"
        )
        return triggered
    }

    private fun evaluateTimeIs(condition: LeafCondition): Boolean {
        // value expected as "HH:mm" string
        val target = condition.value?.toString() ?: return false
        val parts = target.split(":")
        if (parts.size != 2) return false
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val matched = currentHour == (parts[0].toIntOrNull() ?: -1) &&
                currentMinute == (parts[1].toIntOrNull() ?: -1)
        DebugLog.d("TimeEval", "time_is: target=$target, current=$currentHour:$currentMinute, matched=$matched")
        return matched
    }

    private fun evaluateDayOfWeek(condition: LeafCondition): Boolean {
        val target = condition.value?.toString()?.uppercase() ?: return false
        val cal = Calendar.getInstance()
        val dayNames = mapOf(
            Calendar.MONDAY to "MONDAY", Calendar.TUESDAY to "TUESDAY",
            Calendar.WEDNESDAY to "WEDNESDAY", Calendar.THURSDAY to "THURSDAY",
            Calendar.FRIDAY to "FRIDAY", Calendar.SATURDAY to "SATURDAY",
            Calendar.SUNDAY to "SUNDAY"
        )
        val today = dayNames[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        val matched = today == target
        DebugLog.d("TimeEval", "day_of_week: target=$target, today=$today, matched=$matched")
        return matched
    }
}

/** Evaluates location-based conditions. */
class LocationEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "arrive_at", "leave_location", "within_radius"
    )

    override suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        DebugLog.w("LocationEval", "${condition.type}: stub — always false (needs FusedLocation)")
        return false
    }
}

/** Evaluates recurring schedule conditions. */
class RecurringEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf(
        "every_x_hours", "every_x_days", "every_x_weeks",
        "x_times_per_day", "x_times_per_week"
    )

    override suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        DebugLog.w("RecurringEval", "${condition.type}: stub — always false (needs interval tracking)")
        return false
    }
}

/** Evaluates user-defined custom conditions. */
class CustomEvaluator : ConditionEvaluator {
    override val supportedTypes = setOf<String>() // matches anything starting with "custom_"

    override suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        DebugLog.w("CustomEval", "${condition.type}: stub — always false (needs rule engine)")
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
    suspend fun evaluate(condition: Condition, alarmStartedAt: Long): Boolean = when (condition) {
        is LeafCondition -> evaluateLeaf(condition, alarmStartedAt)
        is CompositeCondition -> evaluateComposite(condition, alarmStartedAt)
    }

    private suspend fun evaluateLeaf(leaf: LeafCondition, alarmStartedAt: Long): Boolean {
        if (customEvaluator.canHandle(leaf.type)) {
            return customEvaluator.evaluate(leaf, alarmStartedAt)
        }
        val evaluator = evaluators.firstOrNull { leaf.type in it.supportedTypes }
        if (evaluator == null) {
            DebugLog.e("TreeEval", "No evaluator found for type '${leaf.type}'")
            return false
        }
        return evaluator.evaluate(leaf, alarmStartedAt)
    }

    private suspend fun evaluateComposite(composite: CompositeCondition, alarmStartedAt: Long): Boolean {
        if (composite.children.isEmpty()) return false
        return when (composite.operator) {
            Operator.AND -> composite.children.all { evaluate(it, alarmStartedAt) }
            Operator.OR -> composite.children.any { evaluate(it, alarmStartedAt) }
        }
    }
}
