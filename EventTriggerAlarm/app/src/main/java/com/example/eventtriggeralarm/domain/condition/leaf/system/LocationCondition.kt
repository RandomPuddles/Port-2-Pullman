package com.example.eventtriggeralarm.domain.condition.leaf.system

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Checks whether the device is within a radius of a given location.
 * Requires ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION.
 */
data class LocationCondition(
    override val id: String = UUID.randomUUID().toString(),
    val targetLat: Double,
    val targetLng: Double,
    val radiusMeters: Float,
    val mode: LocationMode,
    val ctx: Context
) : SystemLeafCondition(ctx) {

    enum class LocationMode { INSIDE, OUTSIDE }

    override val label: String
        get() = "Location ${mode.name.lowercase()} ${radiusMeters.toInt()}m radius"

    override suspend fun getCondition(): Boolean {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = suspendCancellableCoroutine<Location?> { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } ?: return false

        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            targetLat,
            targetLng,
            results
        )
        val distanceMeters = results[0]

        return when (mode) {
            LocationMode.INSIDE -> distanceMeters <= radiusMeters
            LocationMode.OUTSIDE -> distanceMeters > radiusMeters
        }
    }
}
