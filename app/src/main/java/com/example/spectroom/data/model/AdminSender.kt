package com.example.spectroom.data.model

import com.example.spectroom.R

object AdminSender {
    const val ID = "admin"
    const val DISPLAY_NAME = "SpectRoom"
    val photoRes: Int = R.drawable.ic_spectroom_logo

    fun isAdmin(userId: String?): Boolean = userId == ID
}
