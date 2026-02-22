package com.example.eventtriggeralarm.domain.condition.leaf.api

import com.example.eventtriggeralarm.domain.condition.Operator
import java.util.UUID

/**
 * Checks weather data from Open-Meteo (free, no API key required).
 * https://open-meteo.com/en/docs
 */
data class WeatherCondition(
    override val id: String = UUID.randomUUID().toString(),
    val weatherField: WeatherField,
    val operator: Operator,
    val value: String,
    val latitude: Double,
    val longitude: Double
) : ApiLeafCondition() {

    enum class WeatherField(val path: String, val displayName: String) {
        TEMPERATURE("current.temperature_2m", "Temperature (°C)"),
        RAIN("current.rain", "Rain (mm)"),
        WIND_SPEED("current.wind_speed_10m", "Wind Speed (km/h)"),
        HUMIDITY("current.relative_humidity_2m", "Humidity (%)"),
        WEATHER_CODE("current.weather_code", "Weather Code")
    }

    override val label: String
        get() = "Weather: ${weatherField.displayName} ${operator.symbol} $value"

    override suspend fun fetchData(): Map<String, Any?> {
        return fetchJson(
            url = "https://api.open-meteo.com/v1/forecast",
            params = mapOf(
                "latitude" to latitude.toString(),
                "longitude" to longitude.toString(),
                "current" to "temperature_2m,rain,wind_speed_10m,relative_humidity_2m,weather_code"
            )
        )
    }

    override fun evaluate(data: Map<String, Any?>): Boolean {
        val actual = resolveField(data, weatherField.path) ?: return false
        return compare(actual, operator, value)
    }
}
