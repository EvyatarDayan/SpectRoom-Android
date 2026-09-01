package com.example.spectroom.domain

import com.example.spectroom.data.model.GenderPreferenceOption
import com.example.spectroom.data.model.MatchBreakdown
import com.example.spectroom.data.model.SpectRoomUser
import com.example.spectroom.data.model.UserProfileStatus
import java.net.URI

/** Sweden bounding box used by iOS for discover / profile location. */
object SwedenLocation {
    const val MIN_LAT = 55.3
    const val MAX_LAT = 69.1
    const val MIN_LON = 10.5
    const val MAX_LON = 24.2
    const val COUNTRY_NAME = "Sweden"
    const val COUNTRY_CODE = "SE"

    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in MIN_LAT..MAX_LAT && longitude in MIN_LON..MAX_LON
}

object ProfileCompletion {
    private const val CHECK_COUNT = 11

    fun evaluate(user: SpectRoomUser): UserProfileStatus =
        if (completionFlags(user).all { it }) UserProfileStatus.COMPLETE else UserProfileStatus.INCOMPLETE

    fun percentage(user: SpectRoomUser): Int {
        val completed = completionFlags(user).count { it }
        return ((completed.toDouble() / CHECK_COUNT) * 100).toInt()
    }

    fun percentage(
        name: String,
        age: Int,
        locationDisplay: String,
        locationLat: Double?,
        locationLon: Double?,
        bio: String,
        profilePhotoURL: String?,
        photos: List<String>,
        interests: List<String>,
        gender: String,
        interestedIn: List<String>,
        lookingFor: List<String>,
        idealFirstDate: List<String>,
    ): Int {
        val checks = listOf(
            hasFullName(name),
            age >= 18,
            hasLocation(locationDisplay, locationLat, locationLon),
            bio.trim().length >= 30,
            hasValidPhotoUrl(profilePhotoURL),
            hasValidGallery(photos),
            interests.size >= 3,
            gender.trim().isNotEmpty(),
            GenderPreferenceOption.hasValidInterestedIn(interestedIn),
            lookingFor.isNotEmpty(),
            idealFirstDate.isNotEmpty(),
        )
        return ((checks.count { it }.toDouble() / CHECK_COUNT) * 100).toInt()
    }

    private fun completionFlags(user: SpectRoomUser): List<Boolean> = listOf(
        hasFullName(user.name),
        user.age >= 18,
        hasLocation(user.location.displayString, user.location.latitude, user.location.longitude),
        user.bio.trim().length >= 30,
        hasValidPhotoUrl(user.profilePhotoURL),
        hasValidGallery(user.photos),
        user.interests.size >= 3,
        user.gender.trim().isNotEmpty(),
        GenderPreferenceOption.hasValidInterestedIn(user.interestedIn),
        user.lookingFor.isNotEmpty(),
        user.idealFirstDate.isNotEmpty(),
    )

    private fun hasFullName(name: String): Boolean =
        name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size >= 2

    private fun hasLocation(display: String, lat: Double?, lon: Double?): Boolean {
        val trimmed = display.trim()
        if (trimmed.isEmpty()) return false
        return if (lat != null && lon != null) {
            SwedenLocation.contains(lat, lon)
        } else {
            true
        }
    }

    private fun hasValidGallery(photos: List<String>): Boolean {
        val valid = photos.count { hasValidPhotoUrl(it) }
        return valid in 2..5
    }

    private fun hasValidPhotoUrl(value: String?): Boolean {
        val trimmed = value?.trim().orEmpty()
        return trimmed.isNotEmpty() && isValidUrl(trimmed)
    }

    private fun isValidUrl(value: String): Boolean = try {
        URI(value).toURL()
        true
    } catch (_: Exception) {
        false
    }
}

object MatchScoring {
    fun calculateBreakdown(
        interests: List<String>,
        age: Int,
        lookingFor: List<String> = emptyList(),
        sensoryPreferences: List<String> = emptyList(),
        shareSensoryPreferences: Boolean = false,
        lifestyle: String = "",
        shareLifestyle: Boolean = false,
        idealFirstDate: List<String> = emptyList(),
        shareIdealFirstDate: Boolean = false,
        currentUser: SpectRoomUser,
    ): MatchBreakdown {
        var matchScore = 0
        val totalFactors = 100

        val currentUserInterests = currentUser.interests.toSet()
        val profileInterests = interests.toSet()
        val commonInterests = currentUserInterests.intersect(profileInterests).sorted()
        val interestsMatch = if (commonInterests.isNotEmpty()) {
            commonInterests.size.toDouble() /
                maxOf(currentUserInterests.size, profileInterests.size).toDouble()
        } else {
            0.0
        }
        matchScore += (interestsMatch * 25).toInt()

        val ageDeltaFromYou = currentUser.age - age
        val ageMatch = maxOf(0.0, 1.0 - kotlin.math.abs(ageDeltaFromYou) / 20.0)
        matchScore += (ageMatch * 20).toInt()

        val currentLookingFor = normalizedLookingFor(currentUser.lookingFor)
        val otherLookingFor = normalizedLookingFor(lookingFor)
        val commonLookingFor = if (currentLookingFor.isNotEmpty() && otherLookingFor.isNotEmpty()) {
            currentLookingFor.intersect(otherLookingFor).sorted()
        } else {
            emptyList()
        }
        if (currentLookingFor.isNotEmpty() && otherLookingFor.isNotEmpty()) {
            val lookingForMatch = if (commonLookingFor.isNotEmpty()) {
                commonLookingFor.size.toDouble() /
                    maxOf(currentLookingFor.size, otherLookingFor.size).toDouble()
            } else {
                0.0
            }
            matchScore += (lookingForMatch * 15).toInt()
        }

        val currentSharesSensory =
            currentUser.shareSensoryPreferences && currentUser.sensoryPreferences.isNotEmpty()
        val otherSharesSensory = shareSensoryPreferences && sensoryPreferences.isNotEmpty()
        val commonSensory = if (currentSharesSensory && otherSharesSensory) {
            currentUser.sensoryPreferences.toSet().intersect(sensoryPreferences.toSet()).sorted()
        } else {
            emptyList()
        }
        if (currentSharesSensory && otherSharesSensory) {
            val sensoryMatch = if (commonSensory.isNotEmpty()) {
                commonSensory.size.toDouble() /
                    maxOf(
                        currentUser.sensoryPreferences.size,
                        sensoryPreferences.size,
                    ).toDouble()
            } else {
                0.0
            }
            matchScore += (sensoryMatch * 15).toInt()
        }

        val sharedLifestyle = sharedLifestyleValue(
            currentLifestyle = currentUser.lifestyle,
            currentShares = currentUser.shareLifestyle,
            otherLifestyle = lifestyle,
            otherShares = shareLifestyle,
        )
        if (sharedLifestyle != null) {
            matchScore += 15
        }

        val currentSharesFirstDate =
            currentUser.shareIdealFirstDate && currentUser.idealFirstDate.isNotEmpty()
        val otherSharesFirstDate = shareIdealFirstDate && idealFirstDate.isNotEmpty()
        val commonIdeal = if (currentSharesFirstDate && otherSharesFirstDate) {
            currentUser.idealFirstDate.toSet().intersect(idealFirstDate.toSet()).sorted()
        } else {
            emptyList()
        }
        if (currentSharesFirstDate && otherSharesFirstDate) {
            val firstDateMatch = if (commonIdeal.isNotEmpty()) {
                commonIdeal.size.toDouble() /
                    maxOf(currentUser.idealFirstDate.size, idealFirstDate.size).toDouble()
            } else {
                0.0
            }
            matchScore += (firstDateMatch * 10).toInt()
        }

        val percentage = ((matchScore.toDouble() / totalFactors) * 100).toInt()
            .coerceIn(0, 95)

        return MatchBreakdown(
            percentage = percentage,
            commonInterests = commonInterests,
            commonLookingFor = commonLookingFor,
            commonSensoryPreferences = commonSensory,
            sharedLifestyle = sharedLifestyle,
            commonIdealFirstDates = commonIdeal,
            ageDeltaFromYou = ageDeltaFromYou,
        )
    }

    fun calculatePercentage(
        interests: List<String>,
        age: Int,
        lookingFor: List<String> = emptyList(),
        sensoryPreferences: List<String> = emptyList(),
        shareSensoryPreferences: Boolean = false,
        lifestyle: String = "",
        shareLifestyle: Boolean = false,
        idealFirstDate: List<String> = emptyList(),
        shareIdealFirstDate: Boolean = false,
        currentUser: SpectRoomUser,
    ): Int = calculateBreakdown(
        interests = interests,
        age = age,
        lookingFor = lookingFor,
        sensoryPreferences = sensoryPreferences,
        shareSensoryPreferences = shareSensoryPreferences,
        lifestyle = lifestyle,
        shareLifestyle = shareLifestyle,
        idealFirstDate = idealFirstDate,
        shareIdealFirstDate = shareIdealFirstDate,
        currentUser = currentUser,
    ).percentage

    private fun normalizedLookingFor(values: List<String>): Set<String> =
        values.mapNotNull { value ->
            when (value) {
                "Romantic relationship", "Relationship" -> "Relationship"
                "In-Person Friendship", "Friendship" -> "Friendship"
                "Online Friendship" -> "Online Friendship"
                "Hobby Buddy" -> "Hobby Buddy"
                "Not Sure Yet", "Not sure yet" -> null
                else -> value.trim().takeIf { it.isNotEmpty() }
            }
        }.toSet()

    private fun sharedLifestyleValue(
        currentLifestyle: String,
        currentShares: Boolean,
        otherLifestyle: String,
        otherShares: Boolean,
    ): String? {
        if (!currentShares || !otherShares) return null
        val current = currentLifestyle.trim()
        val other = otherLifestyle.trim()
        if (current.isEmpty() || other.isEmpty()) return null
        if (current == "preferNot" || other == "preferNot") return null
        return if (current == other) current else null
    }
}

object DiscoverEligibility {
    private const val PASS_COOLDOWN_DAYS = 30L
    private const val FIRST_PASS_COOLDOWN_SECONDS = PASS_COOLDOWN_DAYS * 24 * 60 * 60

    fun isUserEligibleForDeck(
        user: SpectRoomUser,
        currentUser: SpectRoomUser,
        likedUserIds: Set<String>,
        passedUserIds: Set<String>,
        blockedUserIds: Set<String>,
        ignorePassedFilter: Boolean,
        maxDistanceKm: Double?,
    ): Boolean {
        val userId = user.firebaseDocumentId ?: user.id
        if (blockedUserIds.contains(userId)) return false
        if (likedUserIds.contains(userId)) return false
        if (!ignorePassedFilter && passedUserIds.contains(userId)) return false
        if (!matchesDistance(currentUser, user, maxDistanceKm)) return false

        val currentLookingFor = currentUser.lookingFor.filterNot {
            it.equals("Not sure yet", ignoreCase = true) || it.equals("Not Sure Yet", ignoreCase = true)
        }.toSet()

        if (currentLookingFor.isNotEmpty()) {
            val otherLookingFor = user.lookingFor.filterNot {
                it.equals("Not sure yet", ignoreCase = true) || it.equals("Not Sure Yet", ignoreCase = true)
            }.toSet()
            if (otherLookingFor.isNotEmpty() && currentLookingFor.intersect(otherLookingFor).isEmpty()) {
                return false
            }
        }

        if (!matchesGenderPreferences(currentUser, user)) return false
        if (!matchesAgePreferences(currentUser, user)) return false
        return true
    }

    fun matchesAgePreferences(currentUser: SpectRoomUser, otherUser: SpectRoomUser): Boolean {
        val min = currentUser.preferredAgeMin
        val max = currentUser.preferredAgeMax
        if ((min == 0 && max == 0) || max < min) return true
        return otherUser.age in min..max
    }

    fun matchesGenderPreferences(currentUser: SpectRoomUser, otherUser: SpectRoomUser): Boolean {
        if (currentUser.gender.isBlank() ||
            currentUser.interestedIn.isEmpty() ||
            otherUser.gender.isBlank() ||
            otherUser.interestedIn.isEmpty()
        ) {
            return true
        }
        return genderMatches(
            myGender = currentUser.gender,
            myInterestedIn = currentUser.interestedIn,
            theirGender = otherUser.gender,
            theirInterestedIn = otherUser.interestedIn,
        )
    }

    fun matchesDistance(
        currentUser: SpectRoomUser,
        otherUser: SpectRoomUser,
        maxDistanceKm: Double?,
    ): Boolean {
        if (maxDistanceKm == null) return true
        val lat1 = currentUser.location.latitude ?: return false
        val lon1 = currentUser.location.longitude ?: return false
        val lat2 = otherUser.location.latitude ?: return false
        val lon2 = otherUser.location.longitude ?: return false
        return haversineKm(lat1, lon1, lat2, lon2) <= maxDistanceKm
    }

    fun formatDistanceKm(km: Double, forMatches: Boolean = false): String {
        return when {
            forMatches && km < 1 -> "Nearby"
            km < 1 -> "< 1 km"
            else -> "${km.toInt()} km"
        }
    }

    fun lookingForOverlaps(current: List<String>, other: List<String>): Boolean {
        val normalizedCurrent = current.filterNot {
            it.equals("Not Sure Yet", ignoreCase = true) ||
                it.equals("Not sure yet", ignoreCase = true)
        }.toSet()
        val normalizedOther = other.filterNot {
            it.equals("Not Sure Yet", ignoreCase = true) ||
                it.equals("Not sure yet", ignoreCase = true)
        }.toSet()
        if (normalizedCurrent.isEmpty() || normalizedOther.isEmpty()) return true
        return normalizedCurrent.intersect(normalizedOther).isNotEmpty()
    }

    fun genderMatches(
        myGender: String,
        myInterestedIn: List<String>,
        theirGender: String,
        theirInterestedIn: List<String>,
    ): Boolean {
        fun accepts(interestedIn: List<String>, gender: String): Boolean {
            if (interestedIn.any { it.equals("Anyone", ignoreCase = true) }) return true
            return interestedIn.any { it.equals(gender, ignoreCase = true) }
        }
        return accepts(myInterestedIn, theirGender) && accepts(theirInterestedIn, myGender)
    }

    fun isPassStillHidden(
        passCount: Int,
        lastPassAtSeconds: Double,
        nowSeconds: Double = System.currentTimeMillis() / 1000.0,
        allowSecondThoughts: Boolean = false,
    ): Boolean {
        if (allowSecondThoughts) return false
        if (passCount >= 2) return true
        if (passCount == 1) {
            return nowSeconds - lastPassAtSeconds < FIRST_PASS_COOLDOWN_SECONDS
        }
        return false
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
}
