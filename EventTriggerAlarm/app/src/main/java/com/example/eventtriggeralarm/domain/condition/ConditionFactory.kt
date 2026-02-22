package com.example.eventtriggeralarm.domain.condition

import android.content.Context
import com.example.eventtriggeralarm.data.ConditionItem
import com.example.eventtriggeralarm.domain.condition.leaf.api.WeatherCondition
import com.example.eventtriggeralarm.domain.condition.leaf.custom.CustomCondition
import com.example.eventtriggeralarm.domain.condition.leaf.system.BatteryCondition
import com.example.eventtriggeralarm.domain.condition.leaf.system.CalendarCondition
import com.example.eventtriggeralarm.domain.condition.leaf.system.LocationCondition
import com.example.eventtriggeralarm.domain.condition.leaf.system.NetworkCondition
import com.example.eventtriggeralarm.domain.condition.leaf.system.TimeCondition
import com.example.eventtriggeralarm.domain.condition.leaf.StubLeafCondition
import com.example.eventtriggeralarm.gemini.GeminiEvaluator

/**
 * Builds domain [Condition] instances from UI [ConditionItem].
 * Maps item titles to the appropriate leaf condition subclass.
 *
 * @param context Required for system/API conditions.
 * @param geminiEvaluator Required only for CustomCondition; pass null to skip custom items.
 */
object ConditionFactory {

    /** Default coordinates when UI does not provide lat/lng (e.g. Pullman, WA). */
    private const val DEFAULT_LAT = 46.7319
    private const val DEFAULT_LNG = -117.0002

    fun build(
        item: ConditionItem,
        context: Context,
        geminiEvaluator: GeminiEvaluator?
    ): Condition {
        val title = item.title.trim()
        val value = item.value ?: 0.0
        val lat = item.latitude ?: DEFAULT_LAT
        val lng = item.longitude ?: DEFAULT_LNG

        return when {
            // Custom (AI) — requires geminiEvaluator
            item.custom -> {
                if (geminiEvaluator != null) {
                    CustomCondition(prompt = title, geminiEvaluator = geminiEvaluator)
                } else {
                    StubLeafCondition(label = "Custom (no evaluator)", result = false)
                }
            }

            // Device / Battery
            title.equals("Battery below", ignoreCase = true) ->
                BatteryCondition(operator = Operator.LTE, threshold = value.toInt(), ctx = context)
            title.equals("Battery above", ignoreCase = true) ->
                BatteryCondition(operator = Operator.GTE, threshold = value.toInt(), ctx = context)

            // Network
            title.equals("Connected to WiFi", ignoreCase = true) ->
                NetworkCondition(expectedState = NetworkCondition.NetworkState.WIFI, ctx = context)
            title.equals("Bluetooth connected", ignoreCase = true) ->
                StubLeafCondition(label = "Bluetooth (not implemented)", result = false)
            title.equals("Charging", ignoreCase = true) ->
                StubLeafCondition(label = "Charging (not implemented)", result = false)

            // Weather (Open-Meteo returns °C, km/h, mm, %)
            title.equals("Temperature above", ignoreCase = true) -> {
                val celsius = if (item.unit == "°F") (value - 32) * 5 / 9 else value
                WeatherCondition(
                    weatherField = WeatherCondition.WeatherField.TEMPERATURE,
                    operator = Operator.GT,
                    value = celsius.toString(),
                    latitude = lat,
                    longitude = lng
                )
            }
            title.equals("Temperature below", ignoreCase = true) -> {
                val celsius = if (item.unit == "°F") (value - 32) * 5 / 9 else value
                WeatherCondition(
                    weatherField = WeatherCondition.WeatherField.TEMPERATURE,
                    operator = Operator.LT,
                    value = celsius.toString(),
                    latitude = lat,
                    longitude = lng
                )
            }
            title.equals("Rain expected", ignoreCase = true) ->
                WeatherCondition(
                    weatherField = WeatherCondition.WeatherField.RAIN,
                    operator = Operator.GT,
                    value = "0",
                    latitude = lat,
                    longitude = lng
                )
            title.equals("Snow expected", ignoreCase = true) ->
                StubLeafCondition(label = "Snow (not implemented)", result = false)
            title.equals("Wind speed above", ignoreCase = true) -> {
                val kmh = if (item.unit == "mph") value * 1.60934 else value
                WeatherCondition(
                    weatherField = WeatherCondition.WeatherField.WIND_SPEED,
                    operator = Operator.GT,
                    value = kmh.toString(),
                    latitude = lat,
                    longitude = lng
                )
            }
            title.equals("Humidity above", ignoreCase = true) ->
                WeatherCondition(
                    weatherField = WeatherCondition.WeatherField.HUMIDITY,
                    operator = Operator.GTE,
                    value = value.toString(),
                    latitude = lat,
                    longitude = lng
                )

            // Time / Date — TimeCondition supports IN_RANGE, OUT_OF_RANGE, DAY_OF_WEEK
            title.equals("Time is", ignoreCase = true) ->
                StubLeafCondition(label = "Time is (needs HH:MM)", result = false)
            title.equals("Day of week is", ignoreCase = true) ->
                StubLeafCondition(label = "Day of week (needs selection)", result = false)
            title.equals("Date is", ignoreCase = true) ->
                StubLeafCondition(label = "Date is (needs date)", result = false)
            title.equals("Minutes from now", ignoreCase = true) ->
                StubLeafCondition(label = "Minutes from now (not implemented)", result = false)

            // Calendar
            title.equals("Has event", ignoreCase = true) ->
                CalendarCondition(mode = CalendarCondition.CalendarMode.HAS_EVENT, windowMinutes = 60, ctx = context)
            title.equals("Is free", ignoreCase = true) ->
                CalendarCondition(mode = CalendarCondition.CalendarMode.IS_FREE, windowMinutes = 60, ctx = context)

            // Location — requires lat/lng from UI
            title.equals("Arrive at location", ignoreCase = true) -> {
                if (item.latitude != null && item.longitude != null) {
                    LocationCondition(
                        targetLat = item.latitude,
                        targetLng = item.longitude,
                        radiusMeters = 100f,
                        mode = LocationCondition.LocationMode.INSIDE,
                        ctx = context
                    )
                } else {
                    StubLeafCondition(label = "Arrive (needs coordinates)", result = false)
                }
            }
            title.equals("Leave location", ignoreCase = true) -> {
                if (item.latitude != null && item.longitude != null) {
                    LocationCondition(
                        targetLat = item.latitude,
                        targetLng = item.longitude,
                        radiusMeters = 100f,
                        mode = LocationCondition.LocationMode.OUTSIDE,
                        ctx = context
                    )
                } else {
                    StubLeafCondition(label = "Leave (needs coordinates)", result = false)
                }
            }
            title.equals("Within radius of", ignoreCase = true) -> {
                if (item.latitude != null && item.longitude != null) {
                    val meters = (value * 1609.34f).toFloat() // miles → meters
                    LocationCondition(
                        targetLat = item.latitude,
                        targetLng = item.longitude,
                        radiusMeters = meters,
                        mode = LocationCondition.LocationMode.INSIDE,
                        ctx = context
                    )
                } else {
                    StubLeafCondition(label = "Within radius (needs coordinates)", result = false)
                }
            }

            // Recurring — not implemented
            title.startsWith("Every ", ignoreCase = true) ->
                StubLeafCondition(label = "Recurring (not implemented)", result = false)
            title.contains("times per", ignoreCase = true) ->
                StubLeafCondition(label = "Times per (not implemented)", result = false)

            else -> StubLeafCondition(label = "Unknown: $title", result = false)
        }
    }
}
