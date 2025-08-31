package com.example.survey.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // ENHANCED: Get high accuracy location with fresh GPS coordinates
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationWithHighAccuracy(): android.location.Location? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { cont ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                // Only accept location with good accuracy (within 50 meters)
                if (location != null &&
                    location.accuracy <= 50f &&
                    !(location.latitude == 0.0 && location.longitude == 0.0)) {
                    cont.resume(location)
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        cont.resume(lastLocation)
                    }.addOnFailureListener {
                        cont.resume(null)
                    }
                }
            }.addOnFailureListener {
                cont.resume(null)
            }
        }
    }

    // ENHANCED: Get complete detailed address (not just city)
    suspend fun getDetailedAddressFromCoordinates(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]

                // Use full address line first, then build detailed address
                address.getAddressLine(0) ?: buildDetailedAddress(address)
            } else {
                // Fallback to coordinates if geocoding fails
                "${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
            }
        } catch (e: Exception) {
            // Fallback to coordinates on error
            "${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
        }
    }

    // ENHANCED: Build complete address from individual components
    private fun buildDetailedAddress(address: android.location.Address): String {
        return buildString {
            // Street number and name
            if (!address.subThoroughfare.isNullOrBlank()) {
                append(address.subThoroughfare)
                if (!address.thoroughfare.isNullOrBlank()) {
                    append(" ${address.thoroughfare}")
                }
            } else if (!address.thoroughfare.isNullOrBlank()) {
                append(address.thoroughfare)
            }

            // Sub-locality (neighborhood)
            if (!address.subLocality.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append(address.subLocality)
            }

            // Locality (city/town)
            if (!address.locality.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append(address.locality)
            }

            // Admin area (state/province)
            if (!address.adminArea.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append(address.adminArea)
            }

            // Postal code
            if (!address.postalCode.isNullOrBlank()) {
                if (isNotEmpty()) append(" ")
                append(address.postalCode)
            }

            // Country
            if (!address.countryName.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append(address.countryName)
            }
        }.ifBlank { "Unknown Location" }
    }

    // NEW: Main method to get precise location and address
    suspend fun getCurrentLocationAndAddress(): Pair<Pair<Double, Double>, String> {
        val location = getCurrentLocationWithHighAccuracy()

        return if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
            val coordinates = Pair(location.latitude, location.longitude)
            val address = getDetailedAddressFromCoordinates(location.latitude, location.longitude)
            Pair(coordinates, address)
        } else {
            Pair(Pair(0.0, 0.0), "Location unavailable")
        }
    }

    // Legacy method compatibility (for existing code)
    fun getCurrentLocationCoordinates(callback: (Double, Double) -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val location = getCurrentLocationWithHighAccuracy()
            if (location != null) {
                callback(location.latitude, location.longitude)
            } else {
                callback(0.0, 0.0)
            }
        }
    }

    // Legacy method compatibility (for existing code)
    fun getCurrentLocation(callback: (String) -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val (_, address) = getCurrentLocationAndAddress()
            callback(address)
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
