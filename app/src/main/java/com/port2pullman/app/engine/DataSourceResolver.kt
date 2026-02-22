package com.port2pullman.app.engine

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.port2pullman.app.data.TriggerHistoryDao
import com.port2pullman.app.debug.DebugLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Resolves data-source keys (defined in `conditions.json` rules) to
 * live values from the device, system APIs, or computed from the alarm
 * context.
 *
 * Source keys follow the convention `category.metric`, e.g.:
 * - `device.batteryPercent` → battery percentage (Int)
 * - `time.elapsedSeconds`   → seconds since alarm started (Double)
 * - `device.wifiConnected`  → Boolean
 *
 * Returns `null` when the value cannot be obtained (missing permission,
 * unavailable provider, unimplemented source, etc.).
 */
class DataSourceResolver(
    private val context: Context,
    private val triggerHistoryDao: TriggerHistoryDao? = null,
) {

    companion object {
        private const val TAG = "DataSource"
    }

    /**
     * Resolve a single source key.
     * @param key            the data-source key from the JSON rule
     * @param alarmStartedAt epoch-millis when the alarm was last enabled/triggered
     * @param alarmId        the alarm's database ID (needed for limit conditions)
     * @return the resolved value, or `null` on failure
     */
    fun resolve(key: String, alarmStartedAt: Long, alarmId: Long = -1): Any? = try {
        when (key) {
            // ── Device ──────────────────────────────────────────────
            "device.batteryPercent" -> resolveBatteryPercent()
            "device.wifiConnected"  -> resolveWifiConnected()
            "device.wifiSsid"       -> resolveWifiSsid()
            "device.bluetoothConnected" -> resolveBluetoothConnected()
            "device.bluetoothPairedCount" -> resolveBluetoothPairedCount()
            "device.isCharging"     -> resolveIsCharging()
            "device.chargingSource" -> resolveChargingSource()

            // ── Time / Date ─────────────────────────────────────────
            "time.epochMillis"      -> System.currentTimeMillis()
            "time.currentHHmm"      -> resolveCurrentHHmm()
            "time.dayOfWeek"        -> resolveDayOfWeek()
            "time.currentDate"      -> resolveCurrentDate()
            "time.elapsedSeconds"   -> elapsedSince(alarmStartedAt, 1_000.0)
            "time.elapsedMinutes"   -> elapsedSince(alarmStartedAt, 60_000.0)
            "time.elapsedHours"     -> elapsedSince(alarmStartedAt, 3_600_000.0)
            "time.elapsedDays"      -> elapsedSince(alarmStartedAt, 86_400_000.0)
            "time.elapsedWeeks"     -> elapsedSince(alarmStartedAt, 604_800_000.0)

            // ── Location ────────────────────────────────────────────
            "location.latitude"     -> resolveLastLocation()?.first
            "location.longitude"    -> resolveLastLocation()?.second
            "location.accuracy"     -> resolveLocationAccuracy()
            "location.distanceToMi" -> null // needs target coords from condition value

            // ── Weather (Open-Meteo) ──────────────────────────────────
            "weather.temperatureF"    -> { WeatherProvider.ensureFresh(context); WeatherProvider.temperatureF }
            "weather.humidityPercent" -> { WeatherProvider.ensureFresh(context); WeatherProvider.humidityPercent }
            "weather.rainExpected"    -> { WeatherProvider.ensureFresh(context); WeatherProvider.rainExpected }
            "weather.snowExpected"    -> { WeatherProvider.ensureFresh(context); WeatherProvider.snowExpected }
            "weather.windSpeedMph"    -> { WeatherProvider.ensureFresh(context); WeatherProvider.windSpeedMph }
            "weather.rainMm"          -> { WeatherProvider.ensureFresh(context); WeatherProvider.rainMm }
            "weather.snowfallCm"      -> { WeatherProvider.ensureFresh(context); WeatherProvider.snowfallCm }

            // ── Recurring ────────────────────────────────────────────
            "recurring.timesToday"              -> resolveTriggersInPeriod(alarmId, startOfToday())
            "recurring.timesTodayRemaining"     -> null // computed in evaluator from timesToday
            "recurring.timesThisWeek"           -> resolveTriggersInPeriod(alarmId, startOfThisWeek())
            "recurring.timesThisWeekRemaining"  -> null // computed in evaluator from timesThisWeek

            // ── Limits ──────────────────────────────────────────────
            "limit.triggersInLastMinute" -> resolveTriggerCount(alarmId, 60_000L)
            "limit.triggersInLastHour"   -> resolveTriggerCount(alarmId, 3_600_000L)
            "limit.triggersInLastDay"    -> resolveTriggerCount(alarmId, 86_400_000L)
            "limit.triggersInLastWeek"   -> resolveTriggerCount(alarmId, 604_800_000L)
            "limit.triggersInLastMonth"  -> resolveTriggerCount(alarmId, 2_592_000_000L)

            else -> {
                DebugLog.w(TAG, "Unknown source key: $key")
                null
            }
        }
    } catch (e: Exception) {
        DebugLog.e(TAG, "Error resolving '$key'", e)
        null
    }

    // ─── Device helpers ─────────────────────────────────────────────

    private fun resolveBatteryPercent(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun resolveWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @Suppress("DEPRECATION")
    private fun resolveWifiSsid(): String {
        if (!hasPerm("android.permission.ACCESS_WIFI_STATE")) return ""
        val wm = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            ?: return ""
        val ssid = wm.connectionInfo?.ssid
            ?.removePrefix("\"")?.removeSuffix("\"") ?: ""
        return if (ssid == "<unknown ssid>") "(hidden)" else ssid
    }

    @Suppress("MissingPermission")
    private fun resolveBluetoothConnected(): Boolean {
        if (!hasPerm(Manifest.permission.BLUETOOTH_CONNECT)) return false
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return false
        return bm.adapter?.isEnabled == true
    }

    @Suppress("MissingPermission")
    private fun resolveBluetoothPairedCount(): Int {
        if (!hasPerm(Manifest.permission.BLUETOOTH_CONNECT)) return 0
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return 0
        return try { bm.adapter?.bondedDevices?.size ?: 0 } catch (_: Exception) { 0 }
    }

    private fun resolveIsCharging(): Boolean {
        val intent = batteryIntent()
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun resolveChargingSource(): String {
        val intent = batteryIntent()
        return when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "None"
        }
    }

    private fun batteryIntent(): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    // ─── Time helpers ───────────────────────────────────────────────

    private fun resolveCurrentHHmm(): String {
        val cal = Calendar.getInstance()
        return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun resolveDayOfWeek(): String {
        val dayNames = mapOf(
            Calendar.MONDAY to "MONDAY", Calendar.TUESDAY to "TUESDAY",
            Calendar.WEDNESDAY to "WEDNESDAY", Calendar.THURSDAY to "THURSDAY",
            Calendar.FRIDAY to "FRIDAY", Calendar.SATURDAY to "SATURDAY",
            Calendar.SUNDAY to "SUNDAY"
        )
        return dayNames[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)] ?: ""
    }

    private fun resolveCurrentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun elapsedSince(startMs: Long, divisor: Double): Double =
        (System.currentTimeMillis() - startMs) / divisor

    // ─── Location helpers ───────────────────────────────────────────

    @Suppress("MissingPermission")
    private fun resolveLastLocation(): Pair<Double, Double>? {
        if (!hasPerm(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val loc = getBestLocation()
        if (loc != null) return loc.latitude to loc.longitude
        // Fallback: Pullman, WA
        return 46.7298 to -117.1817
    }

    @Suppress("MissingPermission")
    private fun resolveLocationAccuracy(): Float? {
        if (!hasPerm(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        return getBestLocation()?.accuracy ?: 1000f  // fallback ~1km
    }

    /**
     * Try every available location provider for the most recent fix.
     * FUSED_PROVIDER is tried first (Google Play Services), then GPS,
     * NETWORK, and finally PASSIVE.
     */
    @Suppress("MissingPermission")
    private fun getBestLocation(): android.location.Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        for (p in providers) {
            try { lm.getLastKnownLocation(p)?.let { return it } } catch (_: Exception) {}
        }
        return null
    }

    // ─── Utility ────────────────────────────────────────────────────

    private fun hasPerm(perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    // ─── Recurring helpers ───────────────────────────────────────────

    private fun resolveTriggersInPeriod(alarmId: Long, sinceMs: Long): Int? {
        val dao = triggerHistoryDao ?: return null
        if (alarmId < 0) return 0   // probe context — no alarm, return 0
        return dao.countSince(alarmId, sinceMs)
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfThisWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ─── Limit helpers ──────────────────────────────────────────────

    /**
     * Query [TriggerHistoryDao] for number of triggers within [windowMs]
     * milliseconds.  Returns `null` when unavailable (no DAO or no alarm ID).
     */
    private fun resolveTriggerCount(alarmId: Long, windowMs: Long): Int? {
        val dao = triggerHistoryDao ?: run {
            DebugLog.w(TAG, "No TriggerHistoryDao — limit conditions unavailable")
            return null
        }
        if (alarmId < 0) return 0   // probe context — no alarm, return 0
        return dao.countSince(alarmId, System.currentTimeMillis() - windowMs)
    }
}
