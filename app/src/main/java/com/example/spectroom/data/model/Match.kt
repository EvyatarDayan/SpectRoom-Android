package com.example.spectroom.data.model

import java.util.Date
import java.util.UUID

data class Match(
    val id: String = UUID.randomUUID().toString(),
    val user1Id: String,
    val user2Id: String,
    val matchedAt: Date = Date(),
    val isActive: Boolean = true,
    val lastMessageAt: Date? = null,
    val conversationId: String? = null,
)

data class Like(
    val id: String = UUID.randomUUID().toString(),
    val fromUserId: String,
    val toUserId: String,
    val createdAt: Date = Date(),
    val isSuperLike: Boolean = false,
)

data class SwipeAction(
    val id: String = UUID.randomUUID().toString(),
    val fromUserId: String,
    val toUserId: String,
    val action: SwipeType,
    val createdAt: Date = Date(),
    val passCount: Int? = null,
)

enum class SwipeType(val rawValue: String) {
    LIKE("like"),
    PASS("pass"),
    SUPER_LIKE("super_like");

    val displayName: String
        get() = when (this) {
            LIKE -> "Like"
            PASS -> "Pass"
            SUPER_LIKE -> "Super Like"
        }

    companion object {
        fun fromRaw(value: String): SwipeType =
            entries.firstOrNull { it.rawValue == value } ?: PASS
    }
}

data class MatchDisplay(
    val id: String,
    val otherUser: ProfileCard,
    val matchedAt: Date,
    val lastMessageAt: Date? = null,
)

data class ProfileCard(
    val userId: String,
    val name: String,
    val age: Int,
    val bio: String,
    val interests: List<String>,
    val distance: String,
    val imageURL: String?,
    val gender: String,
    val interestedIn: List<String>,
    val lookingFor: List<String>,
    val photos: List<String>,
    val lifestyle: String,
    val shareLifestyle: Boolean,
    val bodyType: String,
    val shareBodyType: Boolean,
    val preferredAgeMin: Int,
    val preferredAgeMax: Int,
    val isOnline: Boolean,
    val lastSeen: Date,
    val idealFirstDate: List<String>,
    val shareIdealFirstDate: Boolean,
    val sensoryPreferences: List<String> = emptyList(),
    val shareSensoryPreferences: Boolean = false,
    val energyLevel: Int = 4,
    val matchPercentage: Int = 0,
)

data class MatchBreakdown(
    val percentage: Int,
    val commonInterests: List<String>,
    val commonLookingFor: List<String>,
    val commonSensoryPreferences: List<String>,
    val sharedLifestyle: String?,
    val commonIdealFirstDates: List<String>,
    /** Positive = you are older; negative = you are younger; 0 = same age. */
    val ageDeltaFromYou: Int,
)
