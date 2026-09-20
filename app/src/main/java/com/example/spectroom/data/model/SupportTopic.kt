package com.example.spectroom.data.model

import java.util.Date

enum class SupportTopic(
    val rawValue: String,
    val labelRes: Int,
    val subjectPrefix: String,
    val isModerationOnly: Boolean = false,
) {
    SAFETY("safety", com.example.spectroom.R.string.support_topic_safety, "[Safety]"),
    BUG("bug", com.example.spectroom.R.string.support_topic_bug, "[Bug]"),
    FEEDBACK("feedback", com.example.spectroom.R.string.support_topic_feedback, "[Feedback]"),
    ACCOUNT_FREEZE(
        "accountFreeze",
        com.example.spectroom.R.string.support_topic_account_freeze,
        "[Account freeze]",
        isModerationOnly = true,
    ),
    ACCOUNT_BAN(
        "accountBan",
        com.example.spectroom.R.string.support_topic_account_ban,
        "[Account ban]",
        isModerationOnly = true,
    ),
    ACCOUNT_DELETED(
        "accountDeleted",
        com.example.spectroom.R.string.support_topic_account_deleted,
        "[Account deleted]",
        isModerationOnly = true,
    ),
    OTHER("other", com.example.spectroom.R.string.support_topic_other, "[Other]");

    companion object {
        val selectableDefault: List<SupportTopic>
            get() = entries.filter { !it.isModerationOnly }

        fun forModerationNotice(notice: ModerationNotice): SupportTopic = when (notice) {
            is ModerationNotice.BanAcknowledgement,
            is ModerationNotice.LoginBanned,
            -> ACCOUNT_BAN
            is ModerationNotice.DeletedAcknowledgement,
            is ModerationNotice.LoginDeleted,
            -> ACCOUNT_DELETED
            is ModerationNotice.RestoredAcknowledgement -> OTHER
            else -> ACCOUNT_FREEZE
        }
    }
}

data class UserReport(
    val id: String,
    val reporterUserId: String,
    val reportedUserId: String,
    val reportedName: String,
    val category: String,
    val details: String?,
    val status: String,
    val createdAt: Date?,
    val bio: String,
    val profilePhotoUrls: List<String>,
) {
    val primaryPhotoUrl: String? get() = profilePhotoUrls.firstOrNull()
}
