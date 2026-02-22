package com.port2pullman.app.model

/**
 * Composite Pattern – sealed condition hierarchy.
 * A condition tree is either a single [LeafCondition] or a
 * [CompositeCondition] that combines children with AND / OR.
 */
sealed class Condition

data class LeafCondition(
    val category: String,
    val type: String,
    val label: String,
    val value: Any? = null
) : Condition()

data class CompositeCondition(
    val operator: Operator,
    val children: List<Condition> = emptyList()
) : Condition()

enum class Operator {
    AND, OR
}
