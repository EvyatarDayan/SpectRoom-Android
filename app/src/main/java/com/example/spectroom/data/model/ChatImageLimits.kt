package com.example.spectroom.data.model

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Mirrors iOS `ChatImageProcessor` chat-image size limits. */
object ChatImageLimits {
    const val MAX_ORIGINAL_BYTES: Long = 5L * 1024L * 1024L
    const val MAX_UPLOAD_BYTES: Long = 5L * 1024L * 1024L

    sealed class ValidationResult {
        data object Ok : ValidationResult()
        data object TooLarge : ValidationResult()
        data object Unavailable : ValidationResult()
    }

    fun validateOriginal(context: Context, uri: Uri): ValidationResult {
        val size = querySizeBytes(context, uri) ?: estimateSizeBytes(context, uri)
        if (size == null) return ValidationResult.Unavailable
        return if (size > MAX_ORIGINAL_BYTES) ValidationResult.TooLarge else ValidationResult.Ok
    }

    private fun querySizeBytes(context: Context, uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index < 0 || !cursor.moveToFirst()) return@use null
                    val value = cursor.getLong(index)
                    value.takeIf { it > 0L }
                }
        }.getOrNull()
    }

    private fun estimateSizeBytes(context: Context, uri: Uri): Long? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                var total = 0L
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_ORIGINAL_BYTES) return@use total
                }
                total
            }
        }.getOrNull()
    }
}
