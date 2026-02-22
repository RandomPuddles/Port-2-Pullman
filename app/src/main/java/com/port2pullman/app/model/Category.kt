package com.port2pullman.app.model

/**
 * A condition category (Weather, Device, etc.) containing
 * selectable [LeafCondition] items.
 */
data class Category(
    val name: String,
    val icon: String,
    val conditions: List<LeafCondition>
)
