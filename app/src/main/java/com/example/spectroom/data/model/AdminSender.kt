package com.example.spectroom.data.model

import com.example.spectroom.R

/** Reserved sender for official SpectRoom messages (admin warnings, etc.). Mirrors iOS `SpectRoomSystemAccount`. */
object AdminSender {
    /** Canonical Firestore sender id used by iOS / server writes. */
    const val ID = "spectroom-official"

    /** Legacy id from older Android writes — still treat as system. */
    const val LEGACY_ID = "admin"

    const val DISPLAY_NAME = "SpectRoom"
    val photoRes: Int = R.drawable.ic_spectroom_logo

    fun isAdmin(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return userId == ID || userId == LEGACY_ID
    }
}
