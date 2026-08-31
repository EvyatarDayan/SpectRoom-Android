package com.example.spectroom.data.local

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundService(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = AppPrefsStore(appContext)
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val matchSoundId: Int =
        soundPool.load(appContext, com.example.spectroom.R.raw.match, 1)
    private val messageSoundId: Int =
        soundPool.load(appContext, com.example.spectroom.R.raw.new_message, 1)

    fun playMatchSound() {
        if (!prefs.soundEnabled) return
        soundPool.play(matchSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playNewMessageSound() {
        if (!prefs.soundEnabled) return
        soundPool.play(messageSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
