package com.example.spectroom.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Disk cache for chat voice messages (mirrors iOS `VoiceCacheService`). */
class VoiceCacheService(context: Context) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Mutex>()

    private val cacheDirectory: File
        get() {
            val dir = File(appContext.cacheDir, "VoiceMessages")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun cachedFile(remoteUrl: String): File? {
        val file = cacheFile(remoteUrl)
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    suspend fun localFile(remoteUrl: String): File = withContext(Dispatchers.IO) {
        if (remoteUrl.startsWith("file:") || remoteUrl.startsWith("/")) {
            return@withContext File(remoteUrl.removePrefix("file://"))
        }
        cachedFile(remoteUrl)?.let { return@withContext it }

        val key = remoteUrl
        val keyMutex = mutex.withLock {
            inFlight.getOrPut(key) { Mutex() }
        }
        keyMutex.withLock {
            cachedFile(remoteUrl)?.let { return@withContext it }
            downloadAndStore(remoteUrl)
        }.also {
            mutex.withLock { inFlight.remove(key) }
        }
    }

    private fun downloadAndStore(remoteUrl: String): File {
        cachedFile(remoteUrl)?.let { return it }
        val connection = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) error("Voice download failed: HTTP $code")
            val bytes = connection.inputStream.use { it.readBytes() }
            require(bytes.isNotEmpty()) { "Empty voice download" }
            val destination = cacheFile(remoteUrl)
            val temp = File(destination.absolutePath + ".tmp")
            temp.writeBytes(bytes)
            if (destination.exists()) destination.delete()
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
            return destination
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(remoteUrl: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(remoteUrl.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val ext = remoteUrl.substringAfterLast('.', missingDelimiterValue = "")
            .substringBefore('?')
            .ifBlank { "m4a" }
            .take(8)
        return File(cacheDirectory, "$digest.$ext")
    }

    companion object {
        @Volatile
        private var instance: VoiceCacheService? = null

        fun getInstance(context: Context): VoiceCacheService {
            return instance ?: synchronized(this) {
                instance ?: VoiceCacheService(context).also { instance = it }
            }
        }
    }
}
