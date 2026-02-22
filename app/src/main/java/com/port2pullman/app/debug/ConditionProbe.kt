package com.port2pullman.app.debug

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.port2pullman.app.data.ConditionRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Probes the device for every value used by condition evaluators and
 * reports the live reading or an error code.
 */
object ConditionProbe {

    enum class Status { OK, STUB, NO_PERMISSION, UNAVAILABLE, ERROR }

    data class ProbeResult(
        val conditionType: String,
        val label: String,
        val category: String,
        val status: Status,
        val value: String,          // human-readable current reading
        val detail: String = "",    // extra context
    )

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    /**
     * Run all probes and return one [ProbeResult] per condition type
     * in the catalog.
     */
    suspend fun probeAll(context: Context): List<ProbeResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ProbeResult>()
        for ((type, def) in ConditionRegistry.definitions) {
            results += when (def.categoryKey) {
                "weather"   -> probeWeather(def)
                "device"    -> probeDevice(context, def)
                "time"      -> probeTime(def)
                "location"  -> probeLocation(context, def)
                "recurring" -> probeRecurring(def)
                else        -> ProbeResult(type, def.label, def.categoryKey,
                    Status.UNAVAILABLE, "—", "Unknown category")
            }
        }
        results
    }

    // ─── Weather ────────────────────────────────────────────────────────

    private fun probeWeather(def: ConditionRegistry.ConditionDef): ProbeResult {
        // Weather requires an external API key – mark as stub
        return ProbeResult(
            conditionType = def.type,
            label = def.label,
            category = "Weather",
            status = Status.STUB,
            value = "—",
            detail = "Needs weather API key (OpenWeatherMap / Tomorrow.io)"
        )
    }

    // ─── Device ─────────────────────────────────────────────────────────

    private fun probeDevice(
        context: Context,
        def: ConditionRegistry.ConditionDef,
    ): ProbeResult = try {
        when (def.type) {
            "battery_below", "battery_above" -> probeBattery(context, def)
            "connected_wifi" -> probeWifi(context, def)
            "bluetooth_connected" -> probeBluetooth(context, def)
            "charging" -> probeCharging(context, def)
            else -> ProbeResult(def.type, def.label, "Device",
                Status.UNAVAILABLE, "—", "Unrecognised device type")
        }
    } catch (e: Exception) {
        ProbeResult(def.type, def.label, "Device", Status.ERROR,
            "ERR", e.message ?: "unknown error")
    }

    private fun probeBattery(
        context: Context,
        def: ConditionRegistry.ConditionDef,
    ): ProbeResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return ProbeResult(def.type, def.label, "Device",
                Status.UNAVAILABLE, "—", "BatteryManager unavailable")
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return ProbeResult(def.type, def.label, "Device",
            Status.OK, "$level%", "Battery level")
    }

    @Suppress("MissingPermission")
    private fun probeWifi(
        context: Context,
        def: ConditionRegistry.ConditionDef,
    ): ProbeResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ProbeResult(def.type, def.label, "Device",
                Status.UNAVAILABLE, "—", "ConnectivityManager unavailable")
        val net = cm.activeNetwork
        val caps = if (net != null) cm.getNetworkCapabilities(net) else null
        val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        // Try to get SSID if possible
        var ssid = ""
        if (hasWifi) {
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                ssid = wm?.connectionInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: ""
                if (ssid == "<unknown ssid>") ssid = "(hidden)"
            } catch (_: Exception) { }
        }

        return ProbeResult(def.type, def.label, "Device",
            Status.OK,
            if (hasWifi) "Connected" else "Not connected",
            if (ssid.isNotEmpty()) "SSID: $ssid" else "")
    }

    private fun probeBluetooth(
        context: Context,
        def: ConditionRegistry.ConditionDef,
    ): ProbeResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return ProbeResult(def.type, def.label, "Device",
                Status.NO_PERMISSION, "—", "BLUETOOTH_CONNECT permission not granted")
        }
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return ProbeResult(def.type, def.label, "Device",
                Status.UNAVAILABLE, "—", "BluetoothManager unavailable")
        val adapter = bm.adapter
            ?: return ProbeResult(def.type, def.label, "Device",
                Status.UNAVAILABLE, "Off", "No Bluetooth adapter")
        val enabled = adapter.isEnabled
        val bonded = if (enabled) {
            try { adapter.bondedDevices?.size ?: 0 } catch (_: Exception) { 0 }
        } else 0
        return ProbeResult(def.type, def.label, "Device",
            Status.OK,
            if (enabled) "Enabled" else "Disabled",
            if (enabled) "$bonded paired device(s)" else "")
    }

    private fun probeCharging(
        context: Context,
        def: ConditionRegistry.ConditionDef,
    ): ProbeResult {
        val intent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "—"
        }
        return ProbeResult(def.type, def.label, "Device",
            Status.OK,
            if (charging) "Charging" else "Not charging",
            if (charging) "Source: $source" else "")
    }

    // ─── Time / Date ────────────────────────────────────────────────────

    private fun probeTime(def: ConditionRegistry.ConditionDef): ProbeResult {
        val cal = Calendar.getInstance()
        return when (def.type) {
            "time_is" -> {
                val now = timeFmt.format(Date())
                ProbeResult(def.type, def.label, "Time / Date",
                    Status.OK, now, "Current device time")
            }
            "day_of_week" -> {
                val dayNames = mapOf(
                    Calendar.MONDAY to "MONDAY", Calendar.TUESDAY to "TUESDAY",
                    Calendar.WEDNESDAY to "WEDNESDAY", Calendar.THURSDAY to "THURSDAY",
                    Calendar.FRIDAY to "FRIDAY", Calendar.SATURDAY to "SATURDAY",
                    Calendar.SUNDAY to "SUNDAY"
                )
                val today = dayNames[cal.get(Calendar.DAY_OF_WEEK)] ?: "?"
                ProbeResult(def.type, def.label, "Time / Date",
                    Status.OK, today, "Calendar.DAY_OF_WEEK")
            }
            "date_is" -> {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                ProbeResult(def.type, def.label, "Time / Date",
                    Status.OK, fmt.format(Date()), "Current date")
            }
            "minutes_from_now" -> {
                val now = System.currentTimeMillis()
                ProbeResult(def.type, def.label, "Time / Date",
                    Status.OK, "epoch=$now",
                    "Reference: lastStartedAt (per alarm)")
            }
            "seconds_from_now" -> {
                val now = System.currentTimeMillis()
                ProbeResult(def.type, def.label, "Time / Date",
                    Status.OK, "epoch=$now",
                    "Reference: lastStartedAt (per alarm)")
            }
            else -> ProbeResult(def.type, def.label, "Time / Date",
                Status.STUB, "—", "Not yet implemented")
        }
    }

    // ─── Location ───────────────────────────────────────────────────────

    private fun probeLocation(
        context: Context,
        def: ConditionRegistry.ConditionDef,
    ): ProbeResult {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            return ProbeResult(def.type, def.label, "Location",
                Status.NO_PERMISSION, "—",
                "ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION not granted")
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return ProbeResult(def.type, def.label, "Location",
                Status.UNAVAILABLE, "—", "LocationManager unavailable")

        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val netEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !netEnabled) {
            return ProbeResult(def.type, def.label, "Location",
                Status.UNAVAILABLE, "Off", "GPS and Network providers disabled")
        }

        // Try to read last known location
        try {
            @Suppress("MissingPermission")
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            return if (loc != null) {
                val age = (System.currentTimeMillis() - loc.time) / 1000
                ProbeResult(def.type, def.label, "Location",
                    Status.OK,
                    "%.5f, %.5f".format(loc.latitude, loc.longitude),
                    "Accuracy: ${loc.accuracy.toInt()}m, age: ${age}s, provider: ${loc.provider}")
            } else {
                ProbeResult(def.type, def.label, "Location",
                    Status.UNAVAILABLE, "—", "No cached location available")
            }
        } catch (e: Exception) {
            return ProbeResult(def.type, def.label, "Location",
                Status.ERROR, "ERR", e.message ?: "unknown")
        }
    }

    // ─── Recurring ──────────────────────────────────────────────────────

    private fun probeRecurring(def: ConditionRegistry.ConditionDef): ProbeResult {
        return ProbeResult(
            conditionType = def.type,
            label = def.label,
            category = "Recurring Schedule",
            status = Status.STUB,
            value = "—",
            detail = "Needs interval tracking / AlarmManager implementation"
        )
    }
}
