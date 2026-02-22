package com.example.eventtriggeralarm.domain.condition.leaf.system

import android.content.Context
import android.os.BatteryManager
import com.example.eventtriggeralarm.domain.condition.Operator
import java.util.UUID

/**
 * Checks the device battery level.
 * No permissions required — BatteryManager is available without permissions.
 */
data class BatteryCondition(
    override val id: String = UUID.randomUUID().toString(),
    val operator: Operator,
    val threshold: Int,
    val ctx: Context
) : SystemLeafCondition(ctx) {

    override val label: String
        get() = "Battery ${operator.symbol} $threshold%"

    override suspend fun getCondition(skipCustom: Boolean): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return compare(level, operator, threshold.toString())
    }
}
