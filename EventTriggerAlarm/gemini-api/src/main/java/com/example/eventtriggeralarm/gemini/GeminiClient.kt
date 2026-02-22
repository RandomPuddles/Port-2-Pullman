package com.example.eventtriggeralarm.gemini

/**
 * Evaluates natural language conditions using Gemini with Google Search grounding.
 * Implementations return true/false based on current real-world information.
 */
interface GeminiEvaluator {
    /**
     * Evaluates a natural language condition prompt.
     * Returns true if the condition is met, false otherwise.
     * Returns false on any error — never throws.
     */
    suspend fun evaluate(prompt: String): Boolean
}
