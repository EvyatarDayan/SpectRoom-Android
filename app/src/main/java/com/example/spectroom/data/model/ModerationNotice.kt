package com.example.spectroom.data.model

import java.util.Date

sealed class ModerationNotice {
    data class Warning(val message: String) : ModerationNotice()
    data class FreezeAcknowledgement(val until: Date, val hours: Int) : ModerationNotice()
    data class LoginBlocked(val until: Date) : ModerationNotice()
    data object BanAcknowledgement : ModerationNotice()
    data object LoginBanned : ModerationNotice()
    data object BanLifted : ModerationNotice()
    data object FreezeLifted : ModerationNotice()
    data object DeletedAcknowledgement : ModerationNotice()
    data object LoginDeleted : ModerationNotice()

    val signsOutOnDismiss: Boolean
        get() = when (this) {
            is FreezeAcknowledgement, BanAcknowledgement, DeletedAcknowledgement -> true
            else -> false
        }
}
