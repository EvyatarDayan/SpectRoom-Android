package com.example.spectroom.data.remote

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VoiceMessageRepository(
    private val context: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L
    private var player: MediaPlayer? = null

    val isRecording: Boolean get() = recorder != null

    fun startRecording(): Result<Unit> = runCatching {
        stopPlayback()
        stopRecordingInternal(discard = true)
        val file = File(context.cacheDir, "voice_${UUID.randomUUID()}.m4a")
        outputFile = file
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioSamplingRate(44100)
        mediaRecorder.setAudioEncodingBitRate(128_000)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()
        recorder = mediaRecorder
        startedAtMs = System.currentTimeMillis()
    }

    fun stopRecording(): Pair<File, Double>? {
        val file = outputFile ?: return null
        val duration = ((System.currentTimeMillis() - startedAtMs) / 1000.0).coerceAtLeast(0.1)
        stopRecordingInternal(discard = false)
        return if (file.exists() && file.length() > 0) file to duration else null
    }

    fun cancelRecording() {
        stopRecordingInternal(discard = true)
    }

    suspend fun uploadVoice(file: File): String = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val path = "voice_messages/voice_${uid}_${UUID.randomUUID()}.m4a"
        val ref = storage.reference.child(path)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("audio/m4a")
            .build()
        ref.putFile(Uri.fromFile(file), metadata).await()
        ref.downloadUrl.await().toString()
    }

    fun playLocalFile(file: File, onCompletion: () -> Unit = {}) {
        stopPlayback()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { start() }
            setOnCompletionListener {
                onCompletion()
                stopPlayback()
            }
            prepareAsync()
        }
    }

    fun play(url: String, onCompletion: () -> Unit = {}) {
        stopPlayback()
        player = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener { start() }
            setOnCompletionListener {
                onCompletion()
                stopPlayback()
            }
            prepareAsync()
        }
    }

    fun stopPlayback() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }

    private fun stopRecordingInternal(discard: Boolean) {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        if (discard) {
            outputFile?.delete()
            outputFile = null
        }
    }
}
