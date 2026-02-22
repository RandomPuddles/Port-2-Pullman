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
 * their labels, metadata (hasNum / unit / placeholder), and category
 * groupings.  To add, remove, or edit a condition you only need to
 * touch the JSON file — no Kotlin changes required.
 */
object ConditionRegistry {

    /** Metadata for a single condition type, mirroring [ConditionMeta.Meta]. */
    data class ConditionDef(
        val categoryKey: String,
        val type: String,
        val label: String,
        val hasNum: Boolean,
        val unit: String,
        val placeholder: String,
    )

    /** Parsed category list (ready for the UI). */
    var categories: List<Category> = emptyList()
        private set

    /** Fast type→metadata look-up, populated from the same JSON. */
    var definitions: Map<String, ConditionDef> = emptyMap()
        private set

    /** Whether [init] has been called. */
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
                    )
                }

                cats += Category(name = catName, icon = catIcon, conditions = leafs)
            }

            categories = cats
            definitions = defs
            loaded = true
        }
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
