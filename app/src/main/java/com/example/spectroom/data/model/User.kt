package com.example.spectroom.data.model

import java.util.Date
import java.util.UUID

data class SpectRoomUser(
    val id: String = UUID.randomUUID().toString(),
    val firebaseDocumentId: String? = null,
    val name: String = "",
    val age: Int = 0,
    val dateOfBirth: Date? = null,
    val email: String = "",
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val profilePhotoURL: String? = null,
    val photos: List<String> = emptyList(),
    val blockedUsers: List<BlockedUser> = emptyList(),
    val blockedByUsers: List<BlockedUser> = emptyList(),
    val location: UserLocation = UserLocation(),
    val preferences: UserPreferences = UserPreferences(),
    val isOnline: Boolean = false,
    val lastSeen: Date = Date(),
    val createdAt: Date = Date(),
    val gender: String = "",
    val interestedIn: List<String> = emptyList(),
    val lookingFor: List<String> = emptyList(),
    val preferredAgeMin: Int = 18,
    val preferredAgeMax: Int = 35,
    val lifestyle: String = "",
    val shareLifestyle: Boolean = false,
    val bodyType: String = "",
    val shareBodyType: Boolean = false,
    val idealFirstDate: List<String> = emptyList(),
    val shareIdealFirstDate: Boolean = false,
    val sensoryPreferences: List<String> = emptyList(),
    val shareSensoryPreferences: Boolean = false,
    /** 0 Low Battery, 2 Moderate Social, 4 Fully Social Mode */
    val energyLevel: Int = 4,
    val planTier: PlanTier = PlanTier.FREE,
    val lastProfileBoostAtMillis: Long? = null,
    val boostedUntilMillis: Long? = null,
)

data class UserLocation(
    val displayString: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class UserPreferences(
    val ageRangeMin: Int = 18,
    val ageRangeMax: Int = 35,
    val maxDistance: Int = 50,
    val showOnlineOnly: Boolean = false,
    val interests: List<String> = emptyList(),
    val communicationStyle: CommunicationStyle = CommunicationStyle.TEXT,
    val notificationSettings: NotificationSettings = NotificationSettings(),
)

enum class CommunicationStyle(val displayName: String) {
    TEXT("Text Only"),
    VOICE("Voice Messages"),
    VIDEO("Video Calls"),
    MIXED("Mixed Communication");

    val description: String
        get() = when (this) {
            TEXT -> "I prefer text-based communication"
            VOICE -> "I'm comfortable with voice messages"
            VIDEO -> "I enjoy video calls"
            MIXED -> "I'm flexible with communication methods"
        }
}

data class NotificationSettings(
    val newMatches: Boolean = true,
    val newMessages: Boolean = true,
    val profileViews: Boolean = false,
    val likes: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

data class BlockedUser(
    val id: String,
    val name: String,
    val blockedAt: Date? = null,
)

enum class UserProfileStatus(val rawValue: String) {
    INCOMPLETE("incomplete"),
    COMPLETE("complete");

    companion object {
        const val FIRESTORE_KEY = "profileStatus"
    }
}
