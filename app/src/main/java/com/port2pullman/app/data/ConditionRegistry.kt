package com.port2pullman.app.data

import android.content.Context
import com.port2pullman.app.R
import com.port2pullman.app.model.Category
import com.port2pullman.app.model.LeafCondition
import org.json.JSONObject

/**
 * Parses the condition catalog from `res/raw/conditions.json`.
 *
 * This is the single source of truth for built-in condition types,
 * their labels, metadata (hasNum / unit / placeholder), evaluation
 * rules, required permissions, and probe keys.  To add, remove, or
 * edit a condition you only need to touch the JSON file.
 */
object ConditionRegistry {

    // ── Rule DSL ─────────────────────────────────────────────────────

    /**
     * A simple comparator rule read from JSON.
     * @param source  data-source key, e.g. `"device.batteryPercent"`
     * @param op      comparison operator: `<`, `>`, `<=`, `>=`, `==`, `!=`
     * @param valueRef either `"user.value"` (use leaf condition value at runtime),
     *                 a literal boolean, or a literal number.
     */
    data class Rule(
        val source: String,
        val op: String,
        val valueRef: Any,          // "user.value" | Boolean | Number
    )

    /** Metadata for a single condition type. */
    data class ConditionDef(
        val categoryKey: String,
        val type: String,
        val label: String,
        val hasNum: Boolean,
        val unit: String,
        val placeholder: String,
        val rule: Rule?,
        val requiresPermissions: List<String>,
        val probeKeys: List<String>,
    )

    /** Parsed category list (ready for the UI). */
    var categories: List<Category> = emptyList()
        private set

    /** Fast type→metadata look-up. */
    var definitions: Map<String, ConditionDef> = emptyMap()
        private set

    @Volatile
    private var loaded = false

    /**
     * Parse the JSON resource.  Safe to call multiple times — only the
     * first invocation does real work.
     */
    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return

            val json = context.resources
                .openRawResource(R.raw.conditions)
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(json)
            val catArray = root.getJSONArray("categories")

            val cats = mutableListOf<Category>()
            val defs = mutableMapOf<String, ConditionDef>()

            for (i in 0 until catArray.length()) {
                val catObj = catArray.getJSONObject(i)
                val catName = catObj.getString("name")
                val catIcon = catObj.getString("icon")
                val catKey = catObj.getString("categoryKey")
                val condArray = catObj.getJSONArray("conditions")

                val leafs = mutableListOf<LeafCondition>()

                for (j in 0 until condArray.length()) {
                    val c = condArray.getJSONObject(j)
                    val type = c.getString("type")
                    val label = c.getString("label")
                    val hasNum = c.optBoolean("hasNum", false)
                    val unit = c.optString("unit", "")
                    val placeholder = c.optString("placeholder", "")

                    // Parse rule object (optional)
                    val rule = c.optJSONObject("rule")?.let { r ->
                        Rule(
                            source = r.getString("source"),
                            op = r.getString("op"),
                            valueRef = parseValueRef(r.get("valueRef")),
                        )
                    }

                    // Parse permissions array
                    val perms = mutableListOf<String>()
                    c.optJSONArray("requiresPermissions")?.let { arr ->
                        for (k in 0 until arr.length()) perms += arr.getString(k)
                    }

                    // Parse probe keys array
                    val probes = mutableListOf<String>()
                    c.optJSONArray("probeKeys")?.let { arr ->
                        for (k in 0 until arr.length()) probes += arr.getString(k)
                    }

                    leafs += LeafCondition(
                        category = catKey,
                        type = type,
                        label = label,
                    )

                    defs[type] = ConditionDef(
                        categoryKey = catKey,
                        type = type,
                        label = label,
                        hasNum = hasNum,
                        unit = unit,
                        placeholder = placeholder,
                        rule = rule,
                        requiresPermissions = perms,
                        probeKeys = probes,
                    )
                }

                cats += Category(name = catName, icon = catIcon, conditions = leafs)
            }

            categories = cats
            definitions = defs
            loaded = true
        }
    }

    /** Convert a JSON valueRef to the appropriate Kotlin type. */
    private fun parseValueRef(raw: Any): Any = when (raw) {
        is Boolean -> raw
        is Number -> raw.toDouble()
        is String -> raw              // e.g. "user.value"
        else -> raw.toString()
    }

    /**
     * Look up metadata by condition type.
     * Returns a sensible default when the type is unknown (e.g. custom_*).
     */
    fun getMeta(type: String): ConditionDef =
        definitions[type] ?: ConditionDef(
            categoryKey = "",
            type = type,
            label = type,
            hasNum = false,
            unit = "",
            placeholder = "",
            rule = null,
            requiresPermissions = emptyList(),
            probeKeys = emptyList(),
        )

    /**
     * Build a prompt-friendly summary of every available condition type,
     * grouped by category.  Used by the AI view-model.
     */
    fun buildAiConditionList(): String = buildString {
        categories.forEach { cat ->
            val types = cat.conditions.joinToString(", ") { (it as LeafCondition).type }
            appendLine("        ${cat.name}: $types")
        }
    }
}
