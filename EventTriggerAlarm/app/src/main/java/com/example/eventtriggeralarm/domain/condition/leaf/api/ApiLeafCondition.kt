package com.example.eventtriggeralarm.domain.condition.leaf.api

import com.example.eventtriggeralarm.domain.condition.LeafCondition
import com.example.eventtriggeralarm.domain.condition.Operator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Base for conditions that require a network call to an external API.
 */
abstract class ApiLeafCondition(
    protected val httpClient: OkHttpClient = DEFAULT_CLIENT
) : LeafCondition() {

    protected fun compare(actual: Any, operator: Operator, expected: String): Boolean {
        return when (operator) {
            Operator.EQUALS -> actual.toString().equals(expected, ignoreCase = true)
            Operator.NOT_EQUALS -> !actual.toString().equals(expected, ignoreCase = true)
            Operator.CONTAINS -> actual.toString().contains(expected, ignoreCase = true)
            Operator.NOT_CONTAINS -> !actual.toString().contains(expected, ignoreCase = true)
            Operator.GT -> (actual.toString().toDoubleOrNull() ?: 0.0) > (expected.toDoubleOrNull() ?: 0.0)
            Operator.LT -> (actual.toString().toDoubleOrNull() ?: 0.0) < (expected.toDoubleOrNull() ?: 0.0)
            Operator.GTE -> (actual.toString().toDoubleOrNull() ?: 0.0) >= (expected.toDoubleOrNull() ?: 0.0)
            Operator.LTE -> (actual.toString().toDoubleOrNull() ?: 0.0) <= (expected.toDoubleOrNull() ?: 0.0)
        }
    }

    protected fun resolveField(data: Map<String, Any?>, path: String): Any? {
        val parts = path.split(".")
        var current: Any? = data
        for (part in parts) {
            current = (current as? Map<*, *>)?.get(part) ?: return null
        }
        return current
    }

    protected suspend fun fetchJson(url: String, params: Map<String, String>): Map<String, Any?> = withContext(Dispatchers.IO) {
        val query = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        val fullUrl = if (params.isEmpty()) url else "$url?$query"
        val request = Request.Builder().url(fullUrl).build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyMap<String, Any?>()
        jsonObjectToMap(JSONObject(body))
    }

    private fun jsonObjectToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        json.keys().forEach { key ->
            val value = json.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonObjectToMap(value)
                else -> value
            }
        }
        return map
    }

    abstract suspend fun fetchData(): Map<String, Any?>

    override suspend fun getCondition(): Boolean {
        return try {
            val data = fetchData()
            evaluate(data)
        } catch (e: Exception) {
            android.util.Log.e(this::class.simpleName, "fetchData failed: ${e.message}")
            false
        }
    }

    protected abstract fun evaluate(data: Map<String, Any?>): Boolean

    companion object {
        internal val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
