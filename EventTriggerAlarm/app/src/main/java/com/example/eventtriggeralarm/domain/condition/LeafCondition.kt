package com.example.eventtriggeralarm.domain.condition

import java.util.UUID

/**
 * Terminal node of the Boolean Expression Tree.
 * Subclasses perform concrete real-world checks (battery, weather, location, etc.).
 */
abstract class LeafCondition(
    override val id: String = UUID.randomUUID().toString()
) : Condition {

    override abstract val label: String
    override abstract suspend fun getCondition(): Boolean
}
