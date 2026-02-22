package com.port2pullman.app.engine

import android.content.Context
import com.port2pullman.app.data.ConditionRegistry
import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.model.*

/**
 * Generic rule evaluator that uses the JSON-defined [ConditionRegistry.Rule]
 * and a [DataSourceResolver] to evaluate any built-in [LeafCondition].
 *
 * When a condition type has no rule definition (e.g. `custom_*`) or the
 * data source returns `null`, evaluation falls through to `false` with
 * appropriate debug logging.
 */
class RuleEvaluator(private val resolver: DataSourceResolver) {

    companion object {
        private const val TAG = "RuleEval"
    }

    /**
     * Evaluate a single [LeafCondition] against its JSON-declared rule.
     */
    suspend fun evaluate(condition: LeafCondition, alarmStartedAt: Long): Boolean {
        val def = ConditionRegistry.getMeta(condition.type)
        val rule = def.rule
        if (rule == null) {
            DebugLog.w(TAG, "${condition.type}: no rule defined — always false")
            return false
        }

        // 1. Resolve the live value from the data source
        val sourceValue = resolver.resolve(rule.source, alarmStartedAt)
        if (sourceValue == null) {
            DebugLog.w(TAG, "${condition.type}: source '${rule.source}' returned null — false")
            return false
        }

        // 2. Resolve the comparison target
        val target = resolveTarget(rule.valueRef, condition)
        if (target == null) {
            DebugLog.w(TAG, "${condition.type}: could not resolve valueRef '${rule.valueRef}' — false")
            return false
        }

        // 3. Apply the comparator
        val result = compare(sourceValue, rule.op, target)

        DebugLog.d(
            TAG,
            "${condition.type}: ${rule.source}=$sourceValue ${rule.op} $target → $result"
        )
        return result
    }

    /**
     * Determine the right-hand side of the comparison.
     * - `"user.value"` → the value from the [LeafCondition] (provided by the user)
     * - literal Boolean / Number → used directly
     */
    private fun resolveTarget(valueRef: Any, condition: LeafCondition): Any? {
        if (valueRef is String && valueRef == "user.value") {
            return condition.value
        }
        return valueRef
    }

    /**
     * Compare [left] to [right] using the operator [op].
     * Works with Numbers (compared as Double), Booleans, and Strings.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compare(left: Any, op: String, right: Any): Boolean {
        // Boolean comparisons
        if (left is Boolean || right is Boolean) {
            val lb = toBool(left)
            val rb = toBool(right)
            return when (op) {
                "==", "eq" -> lb == rb
                "!=", "ne" -> lb != rb
                else -> {
                    DebugLog.w(TAG, "Unsupported op '$op' for booleans")
                    false
                }
            }
        }

        // Try numeric comparison
        val ln = toDouble(left)
        val rn = toDouble(right)
        if (ln != null && rn != null) {
            return when (op) {
                ">"  -> ln > rn
                ">=" -> ln >= rn
                "<"  -> ln < rn
                "<=" -> ln <= rn
                "==" -> ln == rn
                "!=" -> ln != rn
                else -> {
                    DebugLog.w(TAG, "Unsupported numeric op '$op'")
                    false
                }
            }
        }

        // Fall back to string comparison
        val ls = left.toString()
        val rs = right.toString()
        return when (op) {
            "=="          -> ls.equals(rs, ignoreCase = true)
            "!="          -> !ls.equals(rs, ignoreCase = true)
            "contains"    -> ls.contains(rs, ignoreCase = true)
            else -> {
                DebugLog.w(TAG, "Unsupported string op '$op' for '$ls' vs '$rs'")
                false
            }
        }
    }

    private fun toBool(v: Any): Boolean = when (v) {
        is Boolean -> v
        is Number -> v.toDouble() != 0.0
        is String -> v.equals("true", ignoreCase = true) || v == "1"
        else -> false
    }

    private fun toDouble(v: Any): Double? = when (v) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }
}

/**
 * Composite evaluator that applies AND/OR logic for composite conditions
 * and delegates leaf evaluation to [RuleEvaluator].
 */
class ConditionTreeEvaluator(
    private val ruleEvaluator: RuleEvaluator,
) {
    /** Secondary constructor for backward compatibility (service creates with Context). */
    constructor(context: Context) : this(RuleEvaluator(DataSourceResolver(context)))

    suspend fun evaluate(condition: Condition, alarmStartedAt: Long): Boolean = when (condition) {
        is LeafCondition -> ruleEvaluator.evaluate(condition, alarmStartedAt)
        is CompositeCondition -> evaluateComposite(condition, alarmStartedAt)
    }

    private suspend fun evaluateComposite(composite: CompositeCondition, alarmStartedAt: Long): Boolean {
        if (composite.children.isEmpty()) return false
        return when (composite.operator) {
            Operator.AND -> composite.children.all { evaluate(it, alarmStartedAt) }
            Operator.OR -> composite.children.any { evaluate(it, alarmStartedAt) }
        }
    }
}
