package com.example.spectroom.data.model

import java.util.Date

object AdminAccess {
    private val adminEmails = setOf(
        "admin1@yopmail.com",
        "admin2@yopmail.com",
        "admin3@yopmail.com",
    )

    fun isAdmin(email: String?): Boolean {
        val normalized = email?.trim()?.lowercase().orEmpty()
        return normalized.isNotEmpty() && normalized in adminEmails
    }
}

enum class ModerationFreezeDuration(val hours: Int) {
    HOURS_24(24),
    HOURS_48(48),
    HOURS_72(72),
}

enum class ReportModerationAction(val rawValue: String) {
    DISMISS("dismiss"),
    WARNING("warning"),
    FREEZE("freeze"),
    BAN("ban"),
}

data class AdminUserProfile(
    val id: String,
    val name: String,
    val age: Int?,
    val email: String,
    val bio: String,
    val photoUrl: String?,
    val isOnline: Boolean,
    val isBanned: Boolean,
    val isFrozen: Boolean,
    val frozenUntil: Date?,
    val isDeleted: Boolean,
) {
    val titleText: String
        get() = if (age != null && age > 0) "$name, $age" else name
}
