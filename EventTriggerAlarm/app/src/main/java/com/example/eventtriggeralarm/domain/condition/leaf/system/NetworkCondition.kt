package com.example.eventtriggeralarm.domain.condition.leaf.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.UUID

/**
 * Checks the current network connectivity state.
 * Requires ACCESS_NETWORK_STATE permission.
 */
data class NetworkCondition(
    override val id: String = UUID.randomUUID().toString(),
    val expectedState: NetworkState,
    val ctx: Context
) : SystemLeafCondition(ctx) {

    enum class NetworkState { CONNECTED, DISCONNECTED, WIFI, MOBILE_DATA }

    override val label: String
        get() = "Network is ${expectedState.name.lowercase().replace('_', ' ')}"

    override suspend fun getCondition(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return expectedState == NetworkState.DISCONNECTED
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return when (expectedState) {
            NetworkState.CONNECTED -> true
            NetworkState.DISCONNECTED -> false
            NetworkState.WIFI -> caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            NetworkState.MOBILE_DATA -> caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
    }
}
