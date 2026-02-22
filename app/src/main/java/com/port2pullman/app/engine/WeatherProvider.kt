package com.port2pullman.app.engine

import android.content.Context
import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.debug.DebugSettings
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Fetches current weather from the **Open-Meteo** API (free, no API key).
 *
 * Weather data is cached for [CACHE_TTL_MS] (10 min) to avoid
 * hitting the API every 15-second evaluation cycle.
 *
 * All values are read synchronously from the cache by [DataSourceResolver].
 * The cache is refreshed asynchronously when stale.
 */
object WeatherProvider {

    private const val TAG = "Weather"
    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val FALLBACK_LAT = 46.7298               // Pullman, WA
    private const val FALLBACK_LON = -117.1817

    // ── Cached values ───────────────────────────────────────────────
    @Volatile var temperatureF: Double? = null;  private set
    @Volatile var humidityPercent: Double? = null; private set
    @Volatile var rainMm: Double? = null;        private set
    @Volatile var snowfallCm: Double? = null;    private set
    @Volatile var windSpeedMph: Double? = null;  private set
    @Volatile var lastFetchedAt: Long = 0;       private set

    val rainExpected: Boolean?  get() = rainMm?.let { it > 0.0 }
    val snowExpected: Boolean?  get() = snowfallCm?.let { it > 0.0 }

    private val client = HttpClient(Android)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var fetchJob: Job? = null
    private val fetchMutex = Mutex()

    /** True when cache is fresh enough. */
    val isFresh: Boolean get() = System.currentTimeMillis() - lastFetchedAt < DebugSettings.weatherCacheTtlMs

    /**
     * Ensure weather data is available.  If the cache is stale a background
     * fetch is kicked off (non-blocking).  Call from [DataSourceResolver]
     * before reading cached fields.
     */
    fun ensureFresh(context: Context) {
        if (isFresh) return
        if (fetchJob?.isActive == true) return           // already fetching
        fetchJob = scope.launch { fetchWeather(context) }
    }

    /**
     * Blocking fetch — used by the debug probe so values are
     * immediately available after the call returns.
     * @param forceRefresh if true, ignores the cache TTL
     */
    suspend fun fetchNow(context: Context, forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh) return
        // Run the fetch in WeatherProvider's own scope so it survives
        // composition cancellation, but still await the result.
        try {
            withContext(NonCancellable) {
                fetchMutex.withLock {
                    // Re-check freshness after acquiring the lock
                    if (!forceRefresh && isFresh) return@withContext
                    fetchWeather(context)
                }
            }
        } catch (e: CancellationException) {
            // Caller's scope was cancelled — safe to ignore
        }
    }

    // ─────────────────────────────────────────────────────────────────

    private suspend fun fetchWeather(context: Context) {
        try {
            val (lat, lon) = LocationProvider.getLatLon()

            val url = "$BASE_URL?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,rain,snowfall,wind_speed_10m" +
                    "&temperature_unit=fahrenheit&wind_speed_unit=mph"

            DebugLog.d(TAG, "Fetching weather for ($lat, $lon)…")
            val body = client.get(url).bodyAsText()
            val json = JSONObject(body)

            val current = json.getJSONObject("current")
            temperatureF    = current.optDouble("temperature_2m", Double.NaN).takeIf { !it.isNaN() }
            humidityPercent = current.optDouble("relative_humidity_2m", Double.NaN).takeIf { !it.isNaN() }
            rainMm          = current.optDouble("rain", Double.NaN).takeIf { !it.isNaN() }
            snowfallCm      = current.optDouble("snowfall", Double.NaN).takeIf { !it.isNaN() }
            windSpeedMph    = current.optDouble("wind_speed_10m", Double.NaN).takeIf { !it.isNaN() }
            lastFetchedAt   = System.currentTimeMillis()

            DebugLog.i(TAG, "Weather updated — ${temperatureF}°F, ${humidityPercent}%, " +
                    "rain=${rainMm}mm, snow=${snowfallCm}cm, wind=${windSpeedMph}mph")
        } catch (e: Exception) {
            DebugLog.e(TAG, "Weather fetch failed", e)
        }
    }

    // Location is now sourced from LocationProvider singleton
}
