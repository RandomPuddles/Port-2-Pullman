package com.port2pullman.app.debug

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.port2pullman.app.App
import com.port2pullman.app.data.ConditionRegistry
import com.port2pullman.app.engine.DataSourceResolver
import com.port2pullman.app.engine.WeatherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Probes all unique data-source keys from the condition catalog
 * and returns one result **per probeKey** (not per condition).
 */
object ConditionProbe {

    enum class Status { OK, STUB, NO_PERMISSION, ERROR }

    data class ProbeResult(
        val key: String,            // e.g. "weather.temperatureF"
        val category: String,       // e.g. "Weather"
        val status: Status,
        val value: String,          // human-readable current reading
        val detail: String = "",    // extra context (error message, etc.)
    )

    /**
     * Run all probes and return one [ProbeResult] per unique probe key
     * across the entire condition catalog.
     *
     * @param forceRefresh  force-refresh weather cache (ignores TTL)
     */
    suspend fun probeAll(
        context: Context,
        forceRefresh: Boolean = false,
    ): List<ProbeResult> = withContext(Dispatchers.IO) {
        // Force-refresh weather so new location / stale data is updated
        WeatherProvider.fetchNow(context, forceRefresh = forceRefresh)

        val dao = (context.applicationContext as? App)?.triggerHistoryDao
        val resolver = DataSourceResolver(context, dao)

        // Collect every unique probeKey, grouped by category
        // LinkedHashMap preserves insertion order (catalog order)
        val keysByCategory = linkedMapOf<String, MutableSet<String>>()
        val permsByKey = mutableMapOf<String, List<String>>()

        for ((_, def) in ConditionRegistry.definitions) {
            val cat = getCategoryLabel(def.categoryKey)
            val keys = if (def.probeKeys.isNotEmpty()) def.probeKeys
                       else if (def.rule != null) listOf(def.rule.source)
                       else continue

            val set = keysByCategory.getOrPut(cat) { linkedSetOf() }
            for (k in keys) {
                set += k
                // Track the strictest permissions needed for this key
                if (def.requiresPermissions.isNotEmpty() && k !in permsByKey) {
                    permsByKey[k] = def.requiresPermissions
                }
            }
        }

        val results = mutableListOf<ProbeResult>()

        for ((cat, keys) in keysByCategory) {
            for (key in keys) {
                results += probeKey(context, resolver, key, cat, permsByKey[key] ?: emptyList())
            }
        }
        results
    }

    private fun probeKey(
        context: Context,
        resolver: DataSourceResolver,
        key: String,
        category: String,
        requiredPerms: List<String>,
    ): ProbeResult {
        // 1. Permission check
        val missing = requiredPerms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            return ProbeResult(
                key = key,
                category = category,
                status = Status.NO_PERMISSION,
                value = "—",
                detail = "Missing: ${missing.joinToString(", ") { it.substringAfterLast('.') }}"
            )
        }

        // 2. Resolve
        val value = try {
            resolver.resolve(key, System.currentTimeMillis())
        } catch (e: Exception) {
            return ProbeResult(key, category, Status.ERROR, "ERR",
                "${e::class.simpleName}: ${e.message}")
        }

        // 3. Status
        if (value == null) {
            return ProbeResult(key, category, Status.STUB, "null")
        }

        return ProbeResult(key, category, Status.OK, formatValue(value))
    }

    private fun formatValue(value: Any): String = when (value) {
        is Boolean -> if (value) "true" else "false"
        is Float -> "%.2f".format(value)
        is Double -> {
            if (value == value.toLong().toDouble()) value.toLong().toString()
            else "%.2f".format(value)
        }
        is Number -> value.toString()
        else -> value.toString()
    }

    private fun getCategoryLabel(key: String): String = when (key) {
        "weather" -> "Weather"
        "device" -> "Device"
        "time" -> "Time / Date"
        "location" -> "Location"
        "recurring" -> "Recurring"
        "limit" -> "Limit"
        else -> key.replaceFirstChar { it.uppercase() }
    }
}
