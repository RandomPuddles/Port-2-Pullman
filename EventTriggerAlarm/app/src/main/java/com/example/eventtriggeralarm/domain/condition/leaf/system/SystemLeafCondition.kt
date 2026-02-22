package com.example.eventtriggeralarm.domain.condition.leaf.system

import android.content.Context
import com.example.eventtriggeralarm.domain.condition.LeafCondition
import com.example.eventtriggeralarm.domain.condition.Operator

/**
 * Base for conditions that read from Android system services.
 * No network calls.
 */
abstract class SystemLeafCondition(
    protected val context: Context
) : LeafCondition() {

    protected fun compare(actual: Any, operator: Operator, expected: String): Boolean {
        return when (operator) {
            Operator.EQUALS -> actual.toString().equals(expected, ignoreCase = true)
            Operator.NOT_EQUALS -> !actual.toString().equals(expected, ignoreCase = true)
            Operator.CONTAINS -> actual.toString().contains(expected, ignoreCase = true)
            Operator.NOT_CONTAINS -> !actual.toString().contains(expected, ignoreCase = true)
            Operator.GT -> (actual.toString().toDoubleOrNull() ?: 0.0) > (expected.toDoubleOrNull() ?: 0.0)
            Operator.LT -> (actual.toString().toDoubleOrNull() ?: 0.0) < (expected.toDoubleOrNull() ?: 0.0)
            Operator.GTE -> (actual.toString().toDoubleOrNull() ?: 0.0) >= (expected.toDoubleOrNull() ?: 0.0)
            Operator.LTE -> (actual.toString().toDoubleOrNull() ?: 0.0) <= (expected.toDoubleOrNull() ?: 0.0)
        }
    }
}
