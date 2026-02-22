package com.example.eventtriggeralarm.domain.condition

import android.content.Context
import com.example.eventtriggeralarm.data.ConditionItem
import com.example.eventtriggeralarm.gemini.GeminiEvaluator

/**
 * Builds a [CompositeCondition] tree from a flat list of [ConditionItem] and operators.
 * Operators are "AND" or "OR", length = items.size - 1.
 *
 * Parsing: AND binds tighter than OR.
 * Example: [A, B, C] with [AND, OR] → (A AND B) OR C
 */
object TreeBuilder {

    fun buildTree(
        items: List<ConditionItem>,
        operators: List<String>,
        context: Context,
        geminiEvaluator: GeminiEvaluator?
    ): CompositeCondition {
        if (items.isEmpty()) {
            return CompositeCondition(operator = LogicalOperator.AND, children = mutableListOf())
        }
        if (items.size == 1) {
            val leaf = ConditionFactory.build(items[0], context, geminiEvaluator)
            val root = CompositeCondition(operator = LogicalOperator.AND, children = mutableListOf())
            root.add(leaf)
            return root
        }

        val ops = operators.map { if (it.equals("OR", ignoreCase = true)) LogicalOperator.OR else LogicalOperator.AND }
        val leaves = items.map { ConditionFactory.build(it, context, geminiEvaluator) }

        return buildRecursive(leaves, ops, 0, leaves.size - 1)
    }

    /**
     * Build tree for leaves[lo..hi] with operators between them.
     * Splits by OR first (lowest precedence), then AND within each segment.
     */
    private fun buildRecursive(
        leaves: List<Condition>,
        ops: List<LogicalOperator>,
        lo: Int,
        hi: Int
    ): CompositeCondition {
        if (lo == hi) {
            val root = CompositeCondition(operator = LogicalOperator.AND, children = mutableListOf())
            root.add(leaves[lo])
            return root
        }

        // Find rightmost OR (so it's the root: left OR right)
        var orIndex = -1
        for (i in hi - 1 downTo lo) {
            if (ops[i] == LogicalOperator.OR) {
                orIndex = i
                break
            }
        }

        return if (orIndex >= 0) {
            val left = buildRecursive(leaves, ops, lo, orIndex)
            val right = buildRecursive(leaves, ops, orIndex + 1, hi)
            val root = CompositeCondition(operator = LogicalOperator.OR, children = mutableListOf())
            root.add(left)
            root.add(right)
            root
        } else {
            // All ANDs
            val root = CompositeCondition(operator = LogicalOperator.AND, children = mutableListOf())
            for (i in lo..hi) root.add(leaves[i])
            root
        }
    }
}
