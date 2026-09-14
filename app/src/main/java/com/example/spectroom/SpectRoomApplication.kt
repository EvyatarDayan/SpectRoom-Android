package com.example.spectroom

import android.app.Application
import com.example.spectroom.data.local.AppPrefsStore
import com.example.spectroom.data.remote.FirebasePhoneAuthDebug
import com.google.firebase.FirebaseApp
import org.maplibre.android.MapLibre

class SpectRoomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        MapLibre.getInstance(this)
        FirebaseApp.initializeApp(this)
        FirebasePhoneAuthDebug.applyIfNeeded(this)
        AppPrefsStore(this).applyStoredLocale()
    }

    companion object {
        lateinit var instance: SpectRoomApplication
            private set
    }
}
