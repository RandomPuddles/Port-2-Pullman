package com.port2pullman.app.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.port2pullman.app.debug.DebugLog

/**
 * Singleton that actively requests location updates so the app always
 * has a fresh GPS fix.  Without this the app relied on
 * [LocationManager.getLastKnownLocation] which only returns a cached
 * fix (populated by other apps like Google Maps).
 *
 * Call [start] once from a long-lived component (e.g. the foreground
 * service) and [stop] when location is no longer needed.
 */
object LocationProvider {

    private const val TAG = "LocationProvider"

    /** Minimum interval between updates (milliseconds). */
    private const val MIN_INTERVAL_MS = 30_000L   // 30 s

    /** Minimum distance between updates (metres). */
    private const val MIN_DISTANCE_M = 10f         // 10 m

    /** Fallback coordinates — Pullman, WA */
    private const val FALLBACK_LAT = 46.7298
    private const val FALLBACK_LON = -117.1817

    /** The most recent fix received from any provider. */
    @Volatile
    var lastLocation: Location? = null
        private set

    private var started = false

    // One listener per provider so we can remove them cleanly.
    private val listeners = mutableListOf<Pair<String, LocationListener>>()

    /**
     * Begin requesting location updates from all available providers.
     * Safe to call more than once — subsequent calls are no-ops.
     */
    @Suppress("MissingPermission")
    fun start(context: Context) {
        if (started) return

        if (!hasLocationPermission(context)) {
            DebugLog.w(TAG, "No location permission — skipping active updates")
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            DebugLog.e(TAG, "LocationManager unavailable")
            return
        }

        // Seed with best cached fix immediately
        seedFromCached(lm)

        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        )

        for (provider in providers) {
            try {
                if (!lm.isProviderEnabled(provider)) continue

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        onNewFix(location)
                    }

                    @Deprecated("Deprecated in API 29+", ReplaceWith(""))
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
                        // no-op — required override on older API stubs
                    }

                    override fun onProviderEnabled(provider: String) {
                        DebugLog.d(TAG, "Provider enabled: $provider")
                    }

                    override fun onProviderDisabled(provider: String) {
                        DebugLog.d(TAG, "Provider disabled: $provider")
                    }
                }

                lm.requestLocationUpdates(
                    provider,
                    MIN_INTERVAL_MS,
                    MIN_DISTANCE_M,
                    listener,
                    Looper.getMainLooper()
                )
                listeners.add(provider to listener)
                DebugLog.i(TAG, "Registered updates on $provider (${MIN_INTERVAL_MS}ms / ${MIN_DISTANCE_M}m)")
            } catch (e: Exception) {
                DebugLog.w(TAG, "Could not register $provider: ${e.message}")
            }
        }

        started = true
        DebugLog.i(TAG, "Active location updates started (${listeners.size} providers)")
    }

    /**
     * Stop all location updates and release listeners.
     */
    fun stop(context: Context) {
        if (!started) return
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm != null) {
            for ((provider, listener) in listeners) {
                try {
                    lm.removeUpdates(listener)
                    DebugLog.d(TAG, "Removed updates for $provider")
                } catch (e: Exception) {
                    DebugLog.w(TAG, "Error removing listener for $provider: ${e.message}")
                }
            }
        }
        listeners.clear()
        started = false
        DebugLog.i(TAG, "Active location updates stopped")
    }

    // ── Public helpers ───────────────────────────────────────────────

    /**
     * Returns (lat, lon) from the latest active fix, or falls back to
     * Pullman, WA if no fix has been obtained yet.
     */
    fun getLatLon(): Pair<Double, Double> {
        val loc = lastLocation
        return if (loc != null) {
            loc.latitude to loc.longitude
        } else {
            DebugLog.w(TAG, "No active fix — using fallback (Pullman, WA)")
            FALLBACK_LAT to FALLBACK_LON
        }
    }

    /**
     * Returns the accuracy of the latest fix in metres, or a
     * coarse fallback of 1 000 m.
     */
    fun getAccuracy(): Float = lastLocation?.accuracy ?: 1000f

    // ── Internal ─────────────────────────────────────────────────────

    private fun onNewFix(location: Location) {
        val prev = lastLocation
        // Keep the most accurate / most recent fix
        if (prev == null || isBetter(location, prev)) {
            lastLocation = location
            DebugLog.d(
                TAG,
                "New fix: (${location.latitude}, ${location.longitude}) " +
                        "acc=${location.accuracy}m provider=${location.provider}"
            )
        }
    }

    /**
     * Prefer newer fix; if ages are close (<30 s) prefer more accurate.
     */
    private fun isBetter(candidate: Location, current: Location): Boolean {
        val timeDelta = candidate.elapsedRealtimeNanos - current.elapsedRealtimeNanos
        val isSignificantlyNewer = timeDelta > 30_000_000_000L  // 30 s in nanos
        val isSignificantlyOlder = timeDelta < -30_000_000_000L

        return when {
            isSignificantlyNewer -> true
            isSignificantlyOlder -> false
            else -> candidate.accuracy <= current.accuracy // lower = better
        }
    }

    @Suppress("MissingPermission")
    private fun seedFromCached(lm: LocationManager) {
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p)
                if (loc != null) {
                    onNewFix(loc)
                    return
                }
            } catch (_: Exception) { }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
}
