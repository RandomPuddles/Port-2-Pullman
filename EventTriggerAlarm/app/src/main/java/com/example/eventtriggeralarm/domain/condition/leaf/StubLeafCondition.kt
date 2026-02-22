package com.example.eventtriggeralarm.domain.condition.leaf

import com.example.eventtriggeralarm.domain.condition.LeafCondition
import java.util.UUID

/**
 * Simple LeafCondition for testing the tree.
 * Returns a fixed boolean value — no real-world check.
 */
data class StubLeafCondition(
    override val id: String = UUID.randomUUID().toString(),
    override val label: String = "Stub",
    private val result: Boolean = false
) : LeafCondition(id) {

    override suspend fun getCondition(): Boolean = result
}
