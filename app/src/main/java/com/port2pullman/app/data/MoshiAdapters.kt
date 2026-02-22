package com.port2pullman.app.data

import com.port2pullman.app.model.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Polymorphic Moshi adapter for the sealed [Condition] hierarchy.
 * Serialises / deserialises a condition tree where each node carries
 * a `"type"` discriminator (`"leaf"` or `"composite"`).
 */
class ConditionTreeAdapter {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val mapAdapter: JsonAdapter<Map<String, Any?>> =
        moshi.adapter(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )

    fun toJson(condition: Condition): String {
        return mapAdapter.toJson(conditionToMap(condition))
    }

    fun fromJson(json: String): Condition {
        val map = mapAdapter.fromJson(json) ?: throw JsonDataException("Null condition JSON")
        return mapToCondition(map)
    }

    private fun conditionToMap(c: Condition): Map<String, Any?> = when (c) {
        is LeafCondition -> buildMap {
            put("type", "leaf")
            put("category", c.category)
            put("conditionType", c.type)
            put("label", c.label)
            put("value", c.value)
            if (c.negated) put("negated", true)
        }
        is CompositeCondition -> mapOf(
            "type" to "composite",
            "operator" to c.operator.name,
            "children" to c.children.map { conditionToMap(it) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToCondition(map: Map<String, Any?>): Condition {
        return when (map["type"]) {
            "leaf" -> LeafCondition(
                category = map["category"] as? String ?: "",
                type = map["conditionType"] as? String ?: "",
                label = map["label"] as? String ?: "",
                value = map["value"],
                negated = map["negated"] == true
            )
            "composite" -> {
                val opStr = map["operator"] as? String ?: "AND"
                val operator = try { Operator.valueOf(opStr) } catch (_: Exception) { Operator.AND }
                val childMaps = map["children"] as? List<Map<String, Any?>> ?: emptyList()
                CompositeCondition(
                    operator = operator,
                    children = childMaps.map { mapToCondition(it) }
                )
            }
            else -> throw JsonDataException("Unknown condition type: ${map["type"]}")
        }
    }
}

/**
 * Moshi adapter for [RefreshFrequency].
 */
class RefreshFrequencyAdapter {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter: JsonAdapter<Map<String, Any?>> =
        moshi.adapter(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )

    fun toJson(freq: RefreshFrequency): String = adapter.toJson(
        mapOf("value" to freq.value, "unit" to freq.unit.name)
    )

    fun fromJson(json: String): RefreshFrequency {
        val map = adapter.fromJson(json) ?: throw JsonDataException("Null refresh freq JSON")
        val value = (map["value"] as? Number)?.toInt() ?: 1
        val unitStr = map["unit"] as? String ?: "MINUTES"
        val unit = try { TimeUnit.valueOf(unitStr) } catch (_: Exception) { TimeUnit.MINUTES }
        return RefreshFrequency(value, unit)
    }
}

/**
 * Provides a configured [Moshi] instance with all app adapters.
 */
object MoshiProvider {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val conditionTreeAdapter = ConditionTreeAdapter()
    val refreshFrequencyAdapter = RefreshFrequencyAdapter()
}
