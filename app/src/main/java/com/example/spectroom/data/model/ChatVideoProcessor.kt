package com.example.spectroom.data.model

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Mirrors iOS `ChatVideoValidationError`. */
sealed class ChatVideoValidationError : Exception() {
    data object UnsupportedFormat : ChatVideoValidationError()
    data class FileTooLarge(val maxMB: Int) : ChatVideoValidationError()
    data object TooLong : ChatVideoValidationError()
    data object ProcessingFailed : ChatVideoValidationError()
}

/** Mirrors iOS `PreparedChatVideo`. */
data class PreparedChatVideo(
    val videoFile: File,
    val thumbnailJpegBytes: ByteArray,
    val thumbnailFile: File,
    val durationSeconds: Double,
    val contentType: String = "video/mp4",
)

/**
 * Validates chat videos before upload (iOS `ChatVideoProcessor`).
 * Plan byte caps apply to the picked file (same idea as chat images).
 */
object ChatVideoProcessor {
    const val MAX_DURATION_SECONDS: Double = 3 * 60.0

    fun prepareForUpload(
        context: Context,
        sourceUri: Uri,
        plan: PlanTier,
    ): PreparedChatVideo {
        val mime = context.contentResolver.getType(sourceUri)?.lowercase().orEmpty()
        val name = queryDisplayName(context, sourceUri).orEmpty().lowercase()
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
            .ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty() }
            .lowercase()

        if (!isAllowed(mime, ext)) {
            throw ChatVideoValidationError.UnsupportedFormat
        }

        val maxBytes = plan.maxVideoUploadBytes
        val maxMB = plan.maxVideoUploadMegabytes.coerceAtLeast(1)
        val originalSize = querySizeBytes(context, sourceUri)
            ?: throw ChatVideoValidationError.ProcessingFailed
        if (originalSize <= 0L) throw ChatVideoValidationError.ProcessingFailed
        if (originalSize > maxBytes) throw ChatVideoValidationError.FileTooLarge(maxMB)

        val cacheFile = copyToCache(context, sourceUri, ext.ifBlank { "mp4" })
            ?: throw ChatVideoValidationError.ProcessingFailed

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(cacheFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: throw ChatVideoValidationError.ProcessingFailed
            val durationSeconds = durationMs / 1000.0
            if (!durationSeconds.isFinite() || durationSeconds <= 0.2) {
                cacheFile.delete()
                throw ChatVideoValidationError.ProcessingFailed
            }
            if (durationSeconds > MAX_DURATION_SECONDS + 0.25) {
                cacheFile.delete()
                throw ChatVideoValidationError.TooLong
            }

            val frame = retriever.getFrameAtTime(50_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw ChatVideoValidationError.ProcessingFailed
            val thumbBytes = compressThumbnail(frame)
            val thumbFile = File(context.cacheDir, "chat_video_thumb_${UUID.randomUUID()}.jpg")
            FileOutputStream(thumbFile).use { it.write(thumbBytes) }

            if (cacheFile.length() > maxBytes) {
                cacheFile.delete()
                thumbFile.delete()
                throw ChatVideoValidationError.FileTooLarge(maxMB)
            }

            return PreparedChatVideo(
                videoFile = cacheFile,
                thumbnailJpegBytes = thumbBytes,
                thumbnailFile = thumbFile,
                durationSeconds = durationSeconds,
            )
        } catch (error: ChatVideoValidationError) {
            throw error
        } catch (_: Exception) {
            cacheFile.delete()
            throw ChatVideoValidationError.ProcessingFailed
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun isAllowed(mime: String, ext: String): Boolean {
        if (mime.contains("mp4") || mime.contains("quicktime") || mime == "video/webm") return true
        if (mime.startsWith("video/") && (ext in ALLOWED_EXTENSIONS || mime.contains("mpeg"))) return true
        return ext in ALLOWED_EXTENSIONS
    }

    private fun compressThumbnail(bitmap: Bitmap): ByteArray {
        val scaled = scaleDown(bitmap, maxSide = 480)
        var quality = 55
        var bytes: ByteArray
        val stream = java.io.ByteArrayOutputStream()
        do {
            stream.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            bytes = stream.toByteArray()
            quality -= 8
        } while (bytes.size > 22 * 1024 && quality >= 25)
        return bytes
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun copyToCache(context: Context, uri: Uri, ext: String): File? {
        return runCatching {
            val out = File(context.cacheDir, "chat_video_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            } ?: return null
            out.takeIf { it.exists() && it.length() > 0L }
        }.getOrNull()
    }

    private fun querySizeBytes(context: Context, uri: Uri): Long? {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index < 0 || !cursor.moveToFirst()) return@use null
                    cursor.getLong(index).takeIf { it > 0L }
                }
        }.getOrNull()?.let { return it }

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                var total = 0L
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    total += read
                }
                total.takeIf { it > 0L }
            }
        }.getOrNull()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index < 0 || !cursor.moveToFirst()) return@use null
                    cursor.getString(index)
                }
        }.getOrNull()
    }

    private val ALLOWED_EXTENSIONS = setOf("mp4", "mov", "m4v", "webm")
}
