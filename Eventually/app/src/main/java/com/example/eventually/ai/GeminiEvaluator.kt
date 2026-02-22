package com.example.eventually.ai

import android.util.Log
import com.example.eventually.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Evaluates plain-text conditions using Gemini with Google Search grounding.
 * Takes a single condition prompt, returns true/false. Never throws — errors return false.
 */
class GeminiEvaluator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val systemInstruction = """
        You are a condition evaluator for an alarm app.
        The user has set a condition for their alarm. Your job is to determine
        whether that condition is currently true or false in the real world right now.
        
        Use Google Search to find current, up-to-date information relevant to the condition.
        
        Respond ONLY with a JSON object in this exact format:
        { "result": true }
        or
        { "result": false }
        
        Do not include any explanation, reasoning, or additional fields.
        Base your answer strictly on current real-world information from search results.
        If you cannot determine the answer with reasonable confidence, return false.
    """.trimIndent()

    /**
     * Evaluates a plain-text condition using Gemini with Google Search.
     * Returns true if the condition is currently met, false otherwise.
     * Always returns false on error — never throws.
     */
    suspend fun evaluate(conditionPrompt: String): Boolean = withContext(Dispatchers.IO) {
        if (conditionPrompt.isBlank()) return@withContext false
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "GEMINI_API_KEY is not set in local.properties")
            return@withContext false
        }

        return@withContext try {
            val requestBody = buildRequestJson(conditionPrompt)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code} ${response.message}")
                return@withContext false
            }

            val body = response.body?.string() ?: return@withContext false
            parseResult(body)
        } catch (e: Exception) {
            Log.e(TAG, "evaluate() failed: ${e.message}")
            false
        }
    }

    private fun buildRequestJson(conditionPrompt: String): String {
        val escapedPrompt = escapeJson("Evaluate this condition: $conditionPrompt")
        return """
        {
            "contents": [{
                "parts": [{"text": $escapedPrompt}]
            }],
            "systemInstruction": {
                "parts": [{"text": ${escapeJson(systemInstruction)}}]
            },
            "tools": [{"google_search": {}}],
            "generationConfig": {
                "responseMimeType": "application/json",
                "responseSchema": {
                    "type": "OBJECT",
                    "properties": {
                        "result": {"type": "BOOLEAN"}
                    },
                    "required": ["result"]
                }
            }
        }
        """.trimIndent()
    }

    private fun escapeJson(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun parseResult(responseBody: String): Boolean {
        return try {
            val text = json.parseToJsonElement(responseBody)
                .jsonObject["candidates"]
                ?.toString()
                ?.let { json.parseToJsonElement(it) }
                ?.toString()
                ?.trimStart('[')?.trimEnd(']')
                ?.let { json.parseToJsonElement(it) }
                ?.jsonObject?.get("content")
                ?.toString()
                ?.let { json.parseToJsonElement(it) }
                ?.jsonObject?.get("parts")
                ?.toString()
                ?.trimStart('[')?.trimEnd(']')
                ?.let { json.parseToJsonElement(it) }
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content
                ?: return false

            json.parseToJsonElement(text)
                .jsonObject["result"]
                ?.jsonPrimitive
                ?.content
                ?.toBooleanStrictOrNull()
                ?: false
        } catch (e: Exception) {
            Log.e(TAG, "parseResult failed: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "GeminiEvaluator"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
