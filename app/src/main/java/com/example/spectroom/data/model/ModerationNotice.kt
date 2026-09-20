package com.example.spectroom.data.model

import java.util.Date

sealed class ModerationNotice {
    data class Warning(
        val message: String,
        val noticeId: String? = null,
        val createdAtSeconds: Double? = null,
    ) : ModerationNotice()
    data class FreezeAcknowledgement(val until: Date, val hours: Int) : ModerationNotice()
    data class LoginBlocked(val until: Date, val hours: Int = 24) : ModerationNotice()
    data object BanAcknowledgement : ModerationNotice()
    data object LoginBanned : ModerationNotice()
    data object BanLifted : ModerationNotice()
    data object FreezeLifted : ModerationNotice()
    data object DeletedAcknowledgement : ModerationNotice()
    data object LoginDeleted : ModerationNotice()
    /** Signed-out handoff after admin undelete (push). Never signs the user out. */
    data object RestoredAcknowledgement : ModerationNotice()

    val signsOutOnDismiss: Boolean
        get() = when (this) {
            is FreezeAcknowledgement,
            is LoginBlocked,
            BanAcknowledgement,
            LoginBanned,
            DeletedAcknowledgement,
            -> true
            // LoginDeleted is already signed out; Restored stays on auth.
            else -> false
        }
}
