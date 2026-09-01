package com.example.spectroom.data.local

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.spectroom.R
import java.util.concurrent.atomic.AtomicBoolean

class SoundService(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = AppPrefsStore(appContext)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(audioAttributes)
        .build()

    private var matchSoundId: Int = 0
    private var matchSoundLoaded = false
    private val pendingMatchSound = AtomicBoolean(false)
    private var messagePlayer: MediaPlayer? = null

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            if (sampleId != matchSoundId) return@setOnLoadCompleteListener
            matchSoundLoaded = true
            if (pendingMatchSound.getAndSet(false)) {
                playLoadedMatchSound()
            }
        }
        matchSoundId = soundPool.load(appContext, R.raw.match, 1)
    }

    fun playMatchSound() {
        if (!prefs.soundEnabled) return
        if (!matchSoundLoaded) {
            pendingMatchSound.set(true)
            return
        }
        playLoadedMatchSound()
    }

    private fun playLoadedMatchSound() {
        soundPool.play(matchSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playNewMessageSound() {
        if (!prefs.soundEnabled) return
        runCatching {
            messagePlayer?.run {
                runCatching { stop() }
                runCatching { release() }
            }
            messagePlayer = null

            val player = MediaPlayer()
            val assetFd = appContext.resources.openRawResourceFd(R.raw.new_message)
            player.setAudioAttributes(audioAttributes)
            player.setDataSource(assetFd.fileDescriptor, assetFd.startOffset, assetFd.length)
            assetFd.close()
            player.prepare()
            player.setVolume(1f, 1f)
            player.isLooping = false
            messagePlayer = player
            player.setOnCompletionListener { completed ->
                runCatching { completed.release() }
                if (messagePlayer === completed) {
                    messagePlayer = null
                }
            }
            player.setOnErrorListener { errored, _, _ ->
                runCatching { errored.release() }
                if (messagePlayer === errored) {
                    messagePlayer = null
                }
                true
            }
            player.start()
        }
    }

    fun release() {
        runCatching {
            messagePlayer?.stop()
            messagePlayer?.release()
        }
        messagePlayer = null
        soundPool.release()
    }
}
