package com.example.eventtriggeralarm.evaluator

import com.example.eventtriggeralarm.gemini.GeminiEvaluator

/**
 * Stub implementation that always returns false.
 * Use this for testing until a real Gemini integration is configured.
 */
class StubGeminiEvaluator : GeminiEvaluator {
    override suspend fun evaluate(prompt: String): Boolean = false
}
