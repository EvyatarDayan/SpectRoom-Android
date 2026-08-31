package com.example.spectroom.data.remote

import android.content.Context
import android.location.Geocoder
import com.example.spectroom.data.model.UserLocation
import com.example.spectroom.domain.SwedenLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class AddressSuggestion(
    val title: String,
    val subtitle: String,
    val displayString: String,
    val city: String,
    val state: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
)

class AddressSearchRepository(private val context: Context) {
    suspend fun searchSweden(query: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        if (!Geocoder.isPresent()) return@withContext emptyList()

        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocationName(
            "$trimmed, Sweden",
            12,
            SwedenLocation.MIN_LAT,
            SwedenLocation.MIN_LON,
            SwedenLocation.MAX_LAT,
            SwedenLocation.MAX_LON,
        ).orEmpty()

        results.mapNotNull { address ->
            val lat = address.latitude
            val lon = address.longitude
            if (!SwedenLocation.contains(lat, lon) &&
                address.countryCode?.equals(SwedenLocation.COUNTRY_CODE, ignoreCase = true) != true
            ) {
                return@mapNotNull null
            }
            val city = address.locality
                ?: address.subAdminArea
                ?: address.featureName
                ?: ""
            val state = address.adminArea.orEmpty()
            val streetLine = listOfNotNull(address.thoroughfare, address.subThoroughfare)
                .joinToString(" ")
                .trim()
            val title = sequenceOf(streetLine, address.featureName, city)
                .firstOrNull { !it.isNullOrBlank() }
                .orEmpty()
            val subtitle = buildString {
                address.postalCode?.takeIf { it.isNotBlank() }?.let { append(it).append(" ") }
                if (city.isNotBlank() && !city.equals(title, ignoreCase = true)) {
                    append(city)
                }
                if (state.isNotBlank() && !state.equals(city, ignoreCase = true)) {
                    if (isNotEmpty()) append(", ")
                    append(state)
                }
                if (isNotEmpty()) append(", ")
                append(SwedenLocation.COUNTRY_NAME)
            }.trim().trimEnd(',')
            val line = buildString {
                if (title.isNotBlank()) append(title)
                if (subtitle.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(subtitle)
                }
                if (isEmpty()) append(address.getAddressLine(0).orEmpty())
            }.trim().ifBlank { address.getAddressLine(0).orEmpty() }

            AddressSuggestion(
                title = title.ifBlank { line },
                subtitle = subtitle,
                displayString = line,
                city = city,
                state = state,
                country = SwedenLocation.COUNTRY_NAME,
                latitude = lat,
                longitude = lon,
            )
        }.distinctBy { it.displayString.lowercase() }
    }
}

fun AddressSuggestion.toUserLocation(): UserLocation = UserLocation(
    displayString = displayString,
    city = city,
    state = state,
    country = country,
    latitude = latitude,
    longitude = longitude,
)
