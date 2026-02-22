# CustomCondition

`CustomCondition` is a special leaf condition that evaluates a free-text prompt using Gemini with Google Search grounding. It is used when none of the structured conditions can express what the user wants. The model searches the web for current information and returns a strict `true` or `false`.

---

## When to Use

The user should reach `CustomCondition` only if the structured conditions cannot express their intent. In the UI, it appears as the last option in the condition type dropdown, labeled `"Custom (AI)"`. Examples of conditions that require it:

- "Has it not rained for 3 days?"
- "Is there a major traffic incident on I-5?"
- "Is Bitcoin above its 30-day moving average?"
- "Is my favourite team playing tonight?"

---

## Implementation

```kotlin
@JsonClass(generateAdapter = true)
data class CustomCondition(
    override val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val evaluateOnStructuredTrue: Boolean = false,  // the checkbox
    val geminiClient: GeminiClient                  // injected via Hilt
) : LeafCondition() {

    override val label: String
        get() = "AI: \"${prompt.take(40)}${if (prompt.length > 40) "…" else ""}\""

    override suspend fun getCondition(): Boolean {
        return geminiClient.evaluate(prompt)
    }

    @Composable
    override fun BuildUI(onConditionReady: (LeafCondition) -> Unit) {
        var inputText by remember { mutableStateOf("") }
        var checkOnStructuredTrue by remember { mutableStateOf(false) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text(
                text = "Describe your condition in plain language.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Condition") },
                placeholder = { Text("e.g. Has it not rained for 3 days?") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = checkOnStructuredTrue,
                    onCheckedChange = { checkOnStructuredTrue = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Check only when other conditions pass",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Saves AI calls by checking this last",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onConditionReady(
                            copy(
                                prompt = inputText.trim(),
                                evaluateOnStructuredTrue = checkOnStructuredTrue
                            )
                        )
                    }
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Condition")
            }
        }
    }
}
```

---

## GeminiClient

`GeminiClient` wraps the Gemini SDK call with Google Search grounding and enforces a strict JSON output schema.

```kotlin
@Singleton
class GeminiClient @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val client = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,   // from local.properties
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
     * Evaluates a natural language condition prompt.
     * Uses Google Search grounding to get current information.
     * Returns true if the condition is met, false otherwise.
     * Returns false on any error — never throws.
     */
    suspend fun evaluate(prompt: String): Boolean {
        return try {
            val response = client.generateContent(
                content { text("Evaluate this condition: $prompt") }
            )
            val json = response.text ?: return false
            Json.parseToJsonElement(json)
                .jsonObject["result"]
                ?.jsonPrimitive
                ?.booleanOrNull ?: false
        } catch (e: Exception) {
            Log.e("GeminiClient", "evaluate() failed: ${e.message}")
            false
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are a condition evaluator for an alarm app. 
            The user has set a condition for their alarm. Your job is to determine
            whether that condition is currently true or false.
            
            Use Google Search to find current, real-world information relevant to the condition.
            
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

## The evaluateOnStructuredTrue Checkbox

This flag controls when the `CustomCondition` gets evaluated relative to the other conditions in the tree. The logic is applied in `BackgroundEvaluationService`, not inside `getCondition()` itself.

**When `evaluateOnStructuredTrue = false` (default):**
The custom condition is evaluated on the same schedule as all other conditions. It counts as a regular node in the tree.

**When `evaluateOnStructuredTrue = true`:**
The service first evaluates all non-custom conditions. Only if they all pass does it then call `getCondition()` on the custom conditions. This prevents burning Gemini API calls when the cheap structured conditions already fail.

```kotlin
// In BackgroundEvaluationService
private suspend fun evaluateReminder(reminder: Reminder): Boolean {
    val root = reminder.rootCondition as? CompositeCondition ?: return reminder.rootCondition.getCondition()

    // Separate custom conditions that have the flag set
    val (deferredCustom, immediate) = root.children.partition {
        it is CustomCondition && it.evaluateOnStructuredTrue
    }

    // Evaluate all immediate conditions first
    val immediateResult = when (root.operator) {
        LogicalOperator.AND -> immediate.all { it.getCondition() }
        LogicalOperator.OR  -> immediate.any { it.getCondition() }
    }

    // If AND and immediate already failed, skip custom — no point calling Gemini
    if (root.operator == LogicalOperator.AND && !immediateResult) return false

    // Now evaluate deferred custom conditions
    if (deferredCustom.isEmpty()) return immediateResult

    val customResult = when (root.operator) {
        LogicalOperator.AND -> deferredCustom.all { it.getCondition() }
        LogicalOperator.OR  -> deferredCustom.any { it.getCondition() }
    }

    return when (root.operator) {
        LogicalOperator.AND -> immediateResult && customResult
        LogicalOperator.OR  -> immediateResult || customResult
    }
}
```

---

## JSON Schema Enforcement

The Gemini response is enforced via `responseMimeType = "application/json"` and `responseSchema`. This eliminates the need to parse free text or handle unexpected output formats. If the model returns anything other than `{ "result": true/false }`, the parsing falls back to `false`.

---

## Serialization

`CustomCondition` serializes to JSON as:

```json
{
  "type": "CUSTOM",
  "id": "cond-003",
  "prompt": "Has it not rained for 3 days?",
  "evaluateOnStructuredTrue": true
}
```

Note: `geminiClient` is **not** serialized — it is a transient runtime dependency injected by Hilt. When deserializing from Room, the `GeminiClient` is injected by the repository before the condition is used for evaluation.

---

## Error Handling

All errors in `GeminiClient.evaluate()` are caught and return `false`. This includes:
- Network errors
- API key invalid or quota exceeded
- Model returns unexpected JSON
- Model times out

A failed custom condition never causes the alarm to fire erroneously. The trade-off is that a transient API failure will silently skip a firing — consider logging failures to a debug screen for transparency during development.

---

## Dependencies

```kotlin
// build.gradle.kts
implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
```

```
// local.properties
GEMINI_API_KEY=your_key_here
```

```kotlin
// BuildConfig exposure in build.gradle.kts
android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "GEMINI_API_KEY",
            "\"${properties["GEMINI_API_KEY"]}\"")
    }
}
```
