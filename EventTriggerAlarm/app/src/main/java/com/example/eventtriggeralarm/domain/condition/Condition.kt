package com.example.eventtriggeralarm.domain.condition

/**
 * Root of the Boolean Expression Tree.
 * Every node — leaf or composite — implements this interface.
 */
sealed interface Condition {
    val id: String
    val label: String
    suspend fun getCondition(): Boolean
}
