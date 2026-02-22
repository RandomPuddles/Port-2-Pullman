package com.port2pullman.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.port2pullman.app.BuildConfig
import com.port2pullman.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AIUiState(
    val prompt: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val draft: AlarmDraft? = null,
)

class AIViewModel : ViewModel() {

    private val _state = MutableStateFlow(AIUiState())
    val uiState: StateFlow<AIUiState> = _state.asStateFlow()

    private val generativeModel: GenerativeModel? = try {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isNotBlank()) GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = key
        ) else null
    } catch (_: Exception) {
        null
    }

    fun setPrompt(p: String) = _state.update { it.copy(prompt = p) }

    fun generate() {
        val prompt = _state.value.prompt.trim()
        if (prompt.isBlank()) return

        _state.update { it.copy(loading = true, error = null, draft = null) }

        viewModelScope.launch {
            try {
                if (generativeModel != null) {
                    val response = generativeModel.generateContent(buildGeminiPrompt(prompt))
                    val text = response.text ?: ""
                    val draft = parseAIResponse(text, prompt)
                    _state.update { it.copy(loading = false, draft = draft) }
                } else {
                    // Fallback: keyword-based simulation (matches prototype)
                    val draft = simulateAIAlarm(prompt)
                    _state.update { it.copy(loading = false, draft = draft) }
                }
            } catch (e: Exception) {
                // Fallback to simulation on API error
                val draft = simulateAIAlarm(prompt)
                _state.update { it.copy(loading = false, draft = draft, error = null) }
            }
        }
    }

    fun clearDraft() = _state.update { it.copy(draft = null, prompt = "") }

    private fun buildGeminiPrompt(userPrompt: String): String = """
        You are an alarm configuration assistant. Given the user's description, generate a JSON object with:
        - "title": short alarm title
        - "conditions": array of objects with "category", "type", "label", "value" (nullable number)
        - "readout": boolean
        - "ring": boolean
        - "triggerOnce": boolean
        
        Available condition types:
        Weather: temperature_above, temperature_below, rain_expected, snow_expected, wind_speed_above, humidity_above
        Device: battery_below, battery_above, connected_wifi, bluetooth_connected, charging
        Time: time_is, day_of_week, date_is, minutes_from_now
        Location: arrive_at, leave_location, within_radius
        Recurring: every_x_hours, every_x_days, every_x_weeks, x_times_per_day, x_times_per_week
        
        User request: "$userPrompt"
        
        Respond ONLY with the JSON object, no markdown.
    """.trimIndent()

    private fun parseAIResponse(text: String, originalPrompt: String): AlarmDraft {
        return try {
            // Simple JSON extraction
            val json = text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val mapAdapter = moshi.adapter<Map<String, Any?>>(
                com.squareup.moshi.Types.newParameterizedType(
                    Map::class.java, String::class.java, Any::class.java
                )
            )
            val map = mapAdapter.fromJson(json) ?: return simulateAIAlarm(originalPrompt)

            val title = map["title"] as? String ?: originalPrompt.take(40)
            val condList = (map["conditions"] as? List<*>)?.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                LeafCondition(
                    category = m["category"] as? String ?: "",
                    type = m["type"] as? String ?: "",
                    label = m["label"] as? String ?: "",
                    value = m["value"]
                )
            } ?: emptyList()

            val readout = map["readout"] as? Boolean ?: false
            val ring = map["ring"] as? Boolean ?: false
            val triggerOnce = map["triggerOnce"] as? Boolean ?: false

            AlarmDraft(
                title = title,
                rootCondition = CompositeCondition(
                    Operator.AND,
                    condList.ifEmpty { listOf(LeafCondition("time", "time_is", "Time is")) }
                ),
                readout = readout,
                ring = ring,
                triggerOnce = triggerOnce,
            )
        } catch (_: Exception) {
            simulateAIAlarm(originalPrompt)
        }
    }

    /**
     * Keyword-based fallback matching the prototype's simulateAIAlarm().
     */
    private fun simulateAIAlarm(prompt: String): AlarmDraft {
        val lower = prompt.lowercase()
        val conditions = mutableListOf<LeafCondition>()

        if ("rain" in lower)
            conditions += LeafCondition("weather", "rain_expected", "Rain expected")
        if ("cold" in lower || "freez" in lower)
            conditions += LeafCondition("weather", "temperature_below", "Temperature below", 32.0)
        if ("hot" in lower || "heat" in lower)
            conditions += LeafCondition("weather", "temperature_above", "Temperature above", 90.0)
        if ("battery" in lower || "charge" in lower)
            conditions += LeafCondition("device", "battery_below", "Battery below", 20.0)
        if ("morning" in lower)
            conditions += LeafCondition("time", "time_is", "Time is")
        if ("location" in lower || "arrive" in lower || "home" in lower || "work" in lower)
            conditions += LeafCondition("location", "arrive_at", "Arrive at location")
        if ("every" in lower && "hour" in lower)
            conditions += LeafCondition("recurring", "every_x_hours", "Every X hours", 1.0)
        if ("wind" in lower)
            conditions += LeafCondition("weather", "wind_speed_above", "Wind speed above", 25.0)

        if (conditions.isEmpty())
            conditions += LeafCondition("time", "time_is", "Time is")

        return AlarmDraft(
            title = if (prompt.length > 40) prompt.take(40) + "…" else prompt,
            rootCondition = CompositeCondition(Operator.AND, conditions),
            readout = "read" in lower || "speak" in lower || "say" in lower,
            ring = "ring" in lower || "alarm" in lower || "loud" in lower,
            triggerOnce = "once" in lower || "one time" in lower,
        )
    }
}
