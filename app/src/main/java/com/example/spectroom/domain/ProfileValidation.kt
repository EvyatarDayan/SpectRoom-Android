package com.example.spectroom.domain

import com.example.spectroom.data.model.SpectRoomUser
import java.net.URI

enum class ProfileValidationSection {
    PROFILE_PHOTO,
    PHOTOS,
    NAME,
    AGE,
    LOCATION,
    GENDER_PREFERENCES,
    ABOUT_ME,
    IDEAL_FIRST_DATE,
    INTERESTS,
}

data class ProfileValidationResult(
    val isProfilePhotoValid: Boolean,
    val isPhotosValid: Boolean,
    val isNameValid: Boolean,
    val isAgeValid: Boolean,
    val isLocationValid: Boolean,
    val isGenderPreferencesValid: Boolean,
    val isBioValid: Boolean,
    val isIdealFirstDateValid: Boolean,
    val isInterestsValid: Boolean,
) {
    val isFormValid: Boolean
        get() = isProfilePhotoValid &&
            isPhotosValid &&
            isNameValid &&
            isAgeValid &&
            isLocationValid &&
            isBioValid &&
            isInterestsValid &&
            isGenderPreferencesValid &&
            isIdealFirstDateValid

    fun firstMissingSection(): ProfileValidationSection? = when {
        !isProfilePhotoValid -> ProfileValidationSection.PROFILE_PHOTO
        !isPhotosValid -> ProfileValidationSection.PHOTOS
        !isNameValid -> ProfileValidationSection.NAME
        !isAgeValid -> ProfileValidationSection.AGE
        !isLocationValid -> ProfileValidationSection.LOCATION
        !isGenderPreferencesValid -> ProfileValidationSection.GENDER_PREFERENCES
        !isBioValid -> ProfileValidationSection.ABOUT_ME
        !isIdealFirstDateValid -> ProfileValidationSection.IDEAL_FIRST_DATE
        !isInterestsValid -> ProfileValidationSection.INTERESTS
        else -> null
    }
}

object ProfileValidation {
    fun evaluate(
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
    ): ProfileValidationResult = ProfileValidationResult(
        isProfilePhotoValid = hasValidPhotoUrl(profilePhotoURL),
        isPhotosValid = hasValidGallery(photos),
        isNameValid = hasFullName(name),
        isAgeValid = age >= 18,
        isLocationValid = hasLocation(locationDisplay, locationLat, locationLon),
        isGenderPreferencesValid = gender.trim().isNotEmpty() &&
            interestedIn.isNotEmpty() &&
            lookingFor.isNotEmpty(),
        isBioValid = bio.trim().length >= 30,
        isIdealFirstDateValid = idealFirstDate.isNotEmpty(),
        isInterestsValid = interests.size >= 3,
    )

    fun evaluate(user: SpectRoomUser): ProfileValidationResult = evaluate(
        name = user.name,
        age = user.age,
        locationDisplay = user.location.displayString,
        locationLat = user.location.latitude,
        locationLon = user.location.longitude,
        bio = user.bio,
        profilePhotoURL = user.profilePhotoURL,
        photos = user.photos,
        interests = user.interests,
        gender = user.gender,
        interestedIn = user.interestedIn,
        lookingFor = user.lookingFor,
        idealFirstDate = user.idealFirstDate,
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
