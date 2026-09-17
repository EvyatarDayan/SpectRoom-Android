package com.example.spectroom.ui.screens.messages

import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R
import com.example.spectroom.data.remote.PhotoStorageRepository
import kotlinx.coroutines.launch

@Composable
fun ChatVideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isReady by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val storage = remember { PhotoStorageRepository(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoPath(videoUrl)
                        setOnPreparedListener { player ->
                            isReady = true
                            player.isLooping = false
                            start()
                        }
                        setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (!isReady || isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Text(stringResource(R.string.common_done), color = Color.White)
            }
            TextButton(
                onClick = {
                    if (isSaving) return@TextButton
                    isSaving = true
                    scope.launch {
                        val result = storage.saveVideoToGallery(videoUrl)
                        isSaving = false
                        val msg = if (result.isSuccess) {
                            R.string.messages_video_saved_success
                        } else {
                            R.string.messages_video_saved_failed
                        }
                        Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Text(stringResource(R.string.common_save), color = Color.White)
            }
        }
    }

    DisposableEffect(videoUrl) {
        onDispose { }
    }
}
