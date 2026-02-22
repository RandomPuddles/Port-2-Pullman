package com.port2pullman.app.engine

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * Fetches external data (weather, etc.) for condition evaluation.
 * Stub implementation — integrate real APIs as needed.
 */
class ExternalDataFetcher {

    private val client = HttpClient(Android)

    /**
     * Fetch current weather data for a given location.
     * Returns raw JSON response.
     */
    suspend fun fetchWeather(lat: Double, lon: Double, apiKey: String): String {
        val response: HttpResponse = client.get(
            "https://api.openweathermap.org/data/2.5/weather"
        ) {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("appid", apiKey)
            parameter("units", "imperial")
        }
        return response.bodyAsText()
    }

    fun close() {
        client.close()
    }
}
