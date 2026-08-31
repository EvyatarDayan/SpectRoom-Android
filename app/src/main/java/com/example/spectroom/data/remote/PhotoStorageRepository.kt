package com.example.spectroom.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class PhotoStorageRepository(
    private val context: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun uploadProfilePhoto(uri: Uri): String {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        return uploadJpeg(
            uri = uri,
            path = "profile_photos/profile_${uid}_${UUID.randomUUID()}.jpg",
        )
    }

    suspend fun uploadGalleryPhoto(uri: Uri): String {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        return uploadJpeg(
            uri = uri,
            path = "gallery_photos/gallery_${uid}_${UUID.randomUUID()}.jpg",
        )
    }

    suspend fun uploadMessageImage(uri: Uri): String {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        return uploadJpeg(
            uri = uri,
            path = "message_images/message_${uid}_${UUID.randomUUID()}.jpg",
        )
    }

    suspend fun deletePhoto(url: String) = withContext(Dispatchers.IO) {
        runCatching {
            storage.getReferenceFromUrl(url).delete().await()
        }
        Unit
    }

    suspend fun saveImageToGallery(imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = java.net.URL(imageUrl).openConnection()
            connection.connect()
            val bytes = connection.getInputStream().use { it.readBytes() }
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Could not decode image")
            val filename = "SpectRoom_${System.currentTimeMillis()}.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SpectRoom")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create media entry")
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                } ?: error("Could not write image")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val path = MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    bitmap,
                    filename,
                    "SpectRoom chat photo",
                )
                if (path.isNullOrBlank()) error("Could not save image")
            }
            Unit
        }
    }

    private suspend fun uploadJpeg(uri: Uri, path: String): String = withContext(Dispatchers.IO) {
        val bytes = uriToJpegBytes(uri)
        val ref = storage.reference.child(path)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(bytes, metadata).await()
        ref.downloadUrl.await().toString()
    }

    private fun uriToJpegBytes(uri: Uri): ByteArray {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val scaled = scaleDown(bitmap, maxSide = 1600)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
