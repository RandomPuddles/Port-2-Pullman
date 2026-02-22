package com.example.eventtriggeralarm.domain.condition.leaf.custom

import com.example.eventtriggeralarm.domain.condition.LeafCondition
import com.example.eventtriggeralarm.gemini.GeminiEvaluator
import java.util.UUID

/**
 * Evaluates a free-text prompt using Gemini with Google Search grounding.
 * Used when structured conditions cannot express the user's intent.
 *
 * evaluateOnStructuredTrue: when true, the BackgroundEvaluationService evaluates
 * this only after all non-custom conditions pass (saves API calls).
 */
data class CustomCondition(
    override val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val evaluateOnStructuredTrue: Boolean = false,
    private val geminiEvaluator: GeminiEvaluator
) : LeafCondition(id) {

    override val label: String
        get() = "AI: \"${prompt.take(40)}${if (prompt.length > 40) "…" else ""}\""

    override suspend fun getCondition(): Boolean {
        return geminiEvaluator.evaluate(prompt)
    }
}
