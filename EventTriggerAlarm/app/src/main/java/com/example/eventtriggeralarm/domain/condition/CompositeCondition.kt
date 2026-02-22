package com.example.eventtriggeralarm.domain.condition

import java.util.UUID

/**
 * Internal node of the Boolean Expression Tree.
 * Holds a logical operator (AND / OR) and combines its children accordingly.
 */
data class CompositeCondition(
    override val id: String = UUID.randomUUID().toString(),
    val operator: LogicalOperator,
    val children: MutableList<Condition> = mutableListOf()
) : Condition {

    override val label: String
        get() = children.joinToString(" ${operator.name} ") { it.label }

    override suspend fun getCondition(): Boolean {
        if (children.isEmpty()) return false
        return when (operator) {
            LogicalOperator.AND -> children.all { it.getCondition() }
            LogicalOperator.OR -> children.any { it.getCondition() }
        }
    }

    fun add(condition: Condition) {
        children.add(condition)
    }

    fun remove(condition: Condition) {
        children.remove(condition)
    }

    fun removeById(id: String) {
        children.removeAll { it.id == id }
    }
}
