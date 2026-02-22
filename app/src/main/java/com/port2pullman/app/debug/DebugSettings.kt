package com.port2pullman.app.debug

/**
 * Runtime-configurable intervals used by various engine components.
 * Values can be tuned from the Debug Console → Settings tab.
 *
 * Changing a value here takes effect on the **next** cycle/restart
 * of the relevant component; no immediate restart is triggered
 * (except LocationProvider which offers a [restartCallback]).
 */
object DebugSettings {

    // ── Alarm evaluation ────────────────────────────────────────────
    /** AlarmEvaluatorService polling interval (ms). Default 15 s. */
    @Volatile var evalIntervalMs: Long = 15_000L

    // ── GPS / Location ──────────────────────────────────────────────
    /** LocationProvider minimum time between updates (ms). Default 30 s. */
    @Volatile var locationMinTimeMs: Long = 30_000L

    /** LocationProvider minimum distance between updates (m). Default 10 m. */
    @Volatile var locationMinDistanceM: Float = 10f

    // ── Weather ─────────────────────────────────────────────────────
    /** WeatherProvider cache TTL (ms). Default 10 min. */
    @Volatile var weatherCacheTtlMs: Long = 10 * 60_000L

    // ── Debug API tab ───────────────────────────────────────────────
    /** Auto-refresh interval for the API Debug tab (ms). Default 5 s. */
    @Volatile var apiDebugRefreshMs: Long = 5_000L

    /**
     * Optional callback set by [LocationProvider] so the Settings tab
     * can restart location updates after the user changes GPS intervals.
     */
    @Volatile var restartLocationCallback: (() -> Unit)? = null
}
