# Gemini Integration

## Overview

`GeminiEvaluator` is the core of the system. It takes a plain-text condition prompt, sends it to Gemini with Google Search grounding enabled, and returns a `Boolean`. The model searches the web for current information and evaluates whether the condition is met.

---

## GeminiEvaluator

```kotlin
@Singleton
class GeminiEvaluator @Inject constructor() {

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        tools = listOf(Tool(googleSearch = GoogleSearch())),
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema.obj(
                mapOf("result" to Schema.bool())
            )
        },
        systemInstruction = content {
            text(SYSTEM_PROMPT)
        }
    )

    /**
     * Evaluates a plain-text condition using Gemini with Google Search.
     * Returns true if the condition is currently met, false otherwise.
     * Always returns false on error — never throws.
     */
    suspend fun evaluate(conditionPrompt: String): Boolean {
        return try {
            val response = model.generateContent(
                content { text("Evaluate this condition: $conditionPrompt") }
            )
            val json = response.text ?: return false
            Json.parseToJsonElement(json)
                .jsonObject["result"]
                ?.jsonPrimitive
                ?.booleanOrNull ?: false
        } catch (e: Exception) {
            Log.e("GeminiEvaluator", "evaluate() failed: ${e.message}")
            false
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
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
    }
}
```

---

## JSON Enforcement

Gemini is configured with two settings that guarantee structured output:

- `responseMimeType = "application/json"` — forces the response to be valid JSON
- `responseSchema` — constrains the shape to `{ "result": Boolean }` exactly

This means no string parsing, no regex, no fragile text handling. The response is either valid JSON with a boolean result, or the parsing fails and `evaluate()` returns `false` safely.

---

## Google Search Grounding

The `googleSearch` tool is enabled on the model. When Gemini receives a condition prompt it cannot answer from training data alone (current weather, recent news, live scores, etc.), it automatically generates search queries, retrieves results, and grounds its response in that data.

This is why there is no API layer — Gemini with search handles:
- Current weather conditions
- Recent news headlines
- Sports scores and schedules
- Stock prices
- Traffic conditions
- Any other publicly available real-time information

---

## Error Handling

All errors return `false` silently. This means:
- Network failure → condition not met → alarm does not fire
- API quota exceeded → condition not met → alarm does not fire  
- Unexpected response format → condition not met → alarm does not fire

A failed evaluation never causes a false positive (alarm firing when it shouldn't). The trade-off is a possible false negative (alarm not firing during an outage), which is acceptable for this use case.

---

## Example Prompts and Expected Behaviour

| User input | Gemini behaviour |
|---|---|
| "is it currently raining in Seattle?" | Searches current weather for Seattle, returns true/false |
| "has it not rained for 3 days in Seattle?" | Searches recent weather history for Seattle, returns true/false |
| "is Bitcoin above $100,000?" | Searches current BTC price, returns true/false |
| "did the Seahawks win their last game?" | Searches recent game results, returns true/false |
| "is there a major earthquake in Japan?" | Searches recent seismic news, returns true/false |

---

## Dependency Setup

```kotlin
// build.gradle.kts
implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
```

```
// local.properties
GEMINI_API_KEY=your_key_here
```

```kotlin
// build.gradle.kts — expose key via BuildConfig
android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${properties["GEMINI_API_KEY"]}\""
        )
    }
}
```
