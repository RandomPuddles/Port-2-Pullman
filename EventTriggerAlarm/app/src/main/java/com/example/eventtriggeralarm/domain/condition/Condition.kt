package com.example.eventtriggeralarm.domain.condition

/**
 * Root of the Boolean Expression Tree.
 * Every node — leaf or composite — implements this interface.
 */
sealed interface Condition {
    val id: String
    val label: String
    /**
     * @param skipCustom When true, CustomCondition returns true without calling the LLM.
     * Used for Phase 1: check if non-custom conditions would pass before invoking Gemini.
     */
    suspend fun getCondition(skipCustom: Boolean = false): Boolean
}
