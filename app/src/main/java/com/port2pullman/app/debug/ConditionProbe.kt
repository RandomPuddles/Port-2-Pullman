package com.port2pullman.app.debug

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.port2pullman.app.data.ConditionRegistry
import com.port2pullman.app.engine.DataSourceResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Probes the device for every value used by condition evaluators.
 *
 * Uses [DataSourceResolver] (the same resolver the evaluation engine uses)
 * to read live values, and reads `probeKeys` + `requiresPermissions` from
 * the [ConditionRegistry] JSON catalog.
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

    /**
     * Run all probes and return one [ProbeResult] per condition type
     * in the catalog.
     */
    suspend fun probeAll(context: Context): List<ProbeResult> = withContext(Dispatchers.IO) {
        val resolver = DataSourceResolver(context)
        val results = mutableListOf<ProbeResult>()

        for ((type, def) in ConditionRegistry.definitions) {
            results += probeCondition(context, resolver, type, def)
        }
        results
    }

    private fun probeCondition(
        context: Context,
        resolver: DataSourceResolver,
        type: String,
        def: ConditionRegistry.ConditionDef,
    ): ProbeResult {
        val catLabel = ConditionRegistry.definitions.values
            .firstOrNull { it.type == type }
            ?.let { getCategoryLabel(it.categoryKey) } ?: def.categoryKey

        // 1. Check permissions
        val missingPerms = def.requiresPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPerms.isNotEmpty()) {
            return ProbeResult(
                conditionType = type,
                label = def.label,
                category = catLabel,
                status = Status.NO_PERMISSION,
                value = "—",
                detail = "Missing: ${missingPerms.joinToString(", ") { it.substringAfterLast('.') }}"
            )
        }

        // 2. No rule = stub
        if (def.rule == null) {
            return ProbeResult(type, def.label, catLabel,
                Status.STUB, "—", "No rule defined")
        }

        // 3. Resolve all probeKeys and display their values
        val probeKeys = def.probeKeys.ifEmpty { listOf(def.rule.source) }
        val resolvedEntries = mutableListOf<Pair<String, Any?>>()
        var anyNull = false

        for (key in probeKeys) {
            val value = try {
                resolver.resolve(key, System.currentTimeMillis())
            } catch (e: Exception) {
                return ProbeResult(type, def.label, catLabel,
                    Status.ERROR, "ERR", "${e::class.simpleName}: ${e.message}")
            }
            resolvedEntries += key to value
            if (value == null) anyNull = true
        }

        // 4. Determine status
        val status = when {
            resolvedEntries.all { it.second == null } -> Status.STUB
            anyNull -> Status.UNAVAILABLE
            else -> Status.OK
        }

        // Build display value from the primary source
        val primaryValue = resolvedEntries.firstOrNull()?.second
        val displayValue = formatValue(primaryValue, def)

        // Build detail from all resolved probe keys
        val detail = resolvedEntries.joinToString(" | ") { (k, v) ->
            val short = k.substringAfterLast('.')
            "$short=${v ?: "null"}"
        }

        return ProbeResult(type, def.label, catLabel, status, displayValue, detail)
    }

    /**
     * Format a resolved value for human-readable display.
     */
    private fun formatValue(value: Any?, def: ConditionRegistry.ConditionDef): String =
        when {
            value == null -> "—"
            value is Boolean -> if (value) "Yes" else "No"
            value is Number && def.unit.isNotEmpty() -> {
                val n = value.toDouble()
                if (n == n.toLong().toDouble()) "${n.toLong()}${def.unit}"
                else "%.2f%s".format(n, def.unit)
            }
            value is Number -> {
                val n = value.toDouble()
                if (n == n.toLong().toDouble()) "${n.toLong()}"
                else "%.2f".format(n)
            }
            else -> value.toString()
        }

    private fun getCategoryLabel(key: String): String = when (key) {
        "weather" -> "Weather"
        "device" -> "Device"
        "time" -> "Time / Date"
        "location" -> "Location"
        "recurring" -> "Recurring Schedule"
        else -> key.replaceFirstChar { it.uppercase() }
    }
}
