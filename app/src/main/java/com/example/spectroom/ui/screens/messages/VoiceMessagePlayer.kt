package com.example.spectroom.ui.screens.messages

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import com.example.spectroom.ui.theme.AppSecondaryTextLight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.data.remote.VoiceCacheService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicReference

private val VoiceAccent = Color(0xFF128C78)
private val OutgoingBubble = Color(0xFFD9FDD3)
private val PlayedWave = Color.Black.copy(alpha = 0.85f)
private val UnplayedWave = Color.Gray.copy(alpha = 0.35f)
private val ScrubberColor = Color(0xFF73C7F2)

private val activePlayerRef = AtomicReference<MediaPlayer?>(null)

private fun claimPlayer(player: MediaPlayer) {
    val previous = activePlayerRef.getAndSet(player)
    if (previous != null && previous !== player) {
        runCatching {
            if (previous.isPlaying) previous.pause()
        }
    }
}

private fun releaseClaim(player: MediaPlayer?) {
    activePlayerRef.compareAndSet(player, null)
}

@Composable
fun VoiceMessagePlayer(
    audioUrl: String?,
    localFile: File? = null,
    displayDurationSeconds: Double? = null,
    isOutgoing: Boolean = true,
    timestampText: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textColor = if (isOutgoing) Color.Black.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.65f)
    val baseColor = if (isOutgoing) OutgoingBubble else Color.White
    var isLoading by remember(audioUrl, localFile) { mutableStateOf(false) }
    var isPlaying by remember(audioUrl, localFile) { mutableStateOf(false) }
    var loadError by remember(audioUrl, localFile) { mutableStateOf(false) }
    var currentTime by remember(audioUrl, localFile) { mutableFloatStateOf(0f) }
    var duration by remember(audioUrl, localFile) {
        mutableFloatStateOf((displayDurationSeconds ?: 0.0).toFloat())
    }
    var showElapsed by remember(audioUrl, localFile) { mutableStateOf(false) }
    var player by remember(audioUrl, localFile) { mutableStateOf<MediaPlayer?>(null) }
    var resolveJob by remember(audioUrl, localFile) { mutableStateOf<Job?>(null) }

    fun seekTo(progress: Float) {
        val p = player ?: return
        val total = if (p.duration > 0) p.duration else (duration * 1000).toInt()
        if (total <= 0) return
        val ms = (progress.coerceIn(0f, 1f) * total).toInt()
        runCatching { p.seekTo(ms) }
        currentTime = ms / 1000f
        showElapsed = true
    }

    fun stopAndReset() {
        val p = player
        runCatching {
            if (p?.isPlaying == true) p.pause()
            p?.seekTo(0)
        }
        isPlaying = false
        currentTime = 0f
        showElapsed = false
        releaseClaim(p)
    }

    fun prepare(autoPlay: Boolean) {
        if (player != null || isLoading) {
            if (autoPlay) {
                val p = player ?: return
                claimPlayer(p)
                runCatching { p.start() }
                isPlaying = true
                showElapsed = true
            }
            return
        }
        isLoading = true
        loadError = false
        resolveJob?.cancel()
        resolveJob = scope.launch {
            val sourcePath = runCatching {
                when {
                    localFile != null -> localFile.absolutePath
                    audioUrl.isNullOrBlank() -> null
                    audioUrl.startsWith("file:") || audioUrl.startsWith("/") -> audioUrl
                    else -> VoiceCacheService.getInstance(context).localFile(audioUrl).absolutePath
                }
            }.getOrNull()
            if (sourcePath.isNullOrBlank()) {
                isLoading = false
                loadError = true
                return@launch
            }
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            runCatching {
                mediaPlayer.setDataSource(sourcePath)
                mediaPlayer.setOnPreparedListener { prepared ->
                    isLoading = false
                    val secs = prepared.duration / 1000f
                    if (secs > 0f) duration = secs
                    if (autoPlay) {
                        claimPlayer(prepared)
                        prepared.start()
                        isPlaying = true
                        showElapsed = true
                    }
                }
                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    currentTime = 0f
                    showElapsed = false
                    runCatching { it.seekTo(0) }
                    releaseClaim(it)
                }
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    isLoading = false
                    loadError = true
                    isPlaying = false
                    true
                }
                mediaPlayer.prepareAsync()
            }.onFailure {
                isLoading = false
                loadError = true
                mediaPlayer.release()
                player = null
            }
        }
    }

    fun toggle() {
        val p = player
        when {
            p == null -> prepare(autoPlay = true)
            p.isPlaying -> {
                runCatching { p.pause() }
                isPlaying = false
                releaseClaim(p)
            }
            else -> {
                claimPlayer(p)
                runCatching { p.start() }
                isPlaying = true
                showElapsed = true
            }
        }
    }

    LaunchedEffect(audioUrl, localFile) {
        if (duration <= 0f && displayDurationSeconds != null) {
            duration = displayDurationSeconds.toFloat()
        }
        prepare(autoPlay = false)
    }

    LaunchedEffect(isPlaying, player) {
        while (isActive && isPlaying) {
            val p = player
            if (p != null) {
                currentTime = runCatching { p.currentPosition / 1000f }.getOrDefault(currentTime)
                if (p.duration > 0) duration = p.duration / 1000f
            }
            delay(50)
        }
    }

    DisposableEffect(audioUrl, localFile) {
        onDispose {
            resolveJob?.cancel()
            val p = player
            releaseClaim(p)
            runCatching {
                p?.stop()
                p?.release()
            }
            player = null
        }
    }

    val progress = if (duration > 0f) (currentTime / duration).coerceIn(0f, 1f) else 0f
    val displayTime = if (showElapsed || isPlaying || currentTime > 0f) {
        formatVoiceTime(currentTime)
    } else {
        formatVoiceTime(if (duration > 0f) duration else (displayDurationSeconds ?: 0.0).toFloat())
    }

    val bubbleShape = remember(isOutgoing) { ChatBubbleShape(isOutgoing = isOutgoing) }
    Row(
        modifier = modifier
            .widthIn(min = 200.dp, max = 260.dp)
            .clip(bubbleShape)
            .background(baseColor, bubbleShape)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            .padding(
                start = if (isOutgoing) 0.dp else 7.dp,
                end = if (isOutgoing) 7.dp else 0.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { toggle() },
            enabled = !isLoading && !loadError && (!audioUrl.isNullOrBlank() || localFile != null),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(VoiceAccent.copy(alpha = 0.15f)),
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = VoiceAccent,
                )
                loadError -> Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                isPlaying -> Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = VoiceAccent)
                else -> Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = VoiceAccent)
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            VoiceMessageWaveform(
                progress = progress,
                onSeek = { seekTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = displayTime,
                    color = textColor.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!timestampText.isNullOrBlank()) {
                    Text(
                        text = timestampText,
                        color = AppSecondaryTextLight,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceMessageWaveform(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val barCount = 40
    val scrubberPx = with(density) { 11.dp.toPx() }
    val scrubberHitPx = with(density) { 32.dp.toPx() }
    val barHeightsPx = remember(density) {
        val pattern = floatArrayOf(
            6f, 10f, 14f, 8f, 16f, 9f, 12f, 7f, 15f, 10f,
            11f, 8f, 17f, 10f, 13f, 7f, 15f, 9f, 12f, 7f,
            14f, 10f, 11f, 8f, 16f, 9f, 12f, 7f, 15f, 10f,
            11f, 7f, 14f, 9f, 16f, 8f, 12f, 7f, 13f, 9f,
        )
        pattern.map { with(density) { it.dp.toPx() } }.toFloatArray()
    }
    val thinBarPx = with(density) { 2.dp.toPx() }
    val thickBarPx = with(density) { 2.5.dp.toPx() }
    var dragStart by remember { mutableStateOf<Float?>(null) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val step = size.width / (barCount - 1).coerceAtLeast(1)
            val verticalInset = with(density) { 2.dp.toPx() }
            val maxBarHeight = (size.height - verticalInset * 2f).coerceAtLeast(1f)
            for (index in 0 until barCount) {
                val normalized = index.toFloat() / (barCount - 1).coerceAtLeast(1)
                val played = normalized <= progress
                val barW = if (index % 4 == 0) thickBarPx else thinBarPx
                val barH = barHeightsPx[index % barHeightsPx.size].coerceAtMost(maxBarHeight)
                val x = step * index
                drawRoundRect(
                    color = if (played) PlayedWave else UnplayedWave,
                    topLeft = Offset(x - barW / 2f, (size.height - barH) / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                )
            }
        }
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val touchX = offset.x.coerceIn(0f, width)
                            val touchProgress = (touchX / width).coerceIn(0f, 1f)
                            val scrubberX = width * progress.coerceIn(0f, 1f)
                            if (kotlin.math.abs(touchX - scrubberX) <= scrubberHitPx / 2f) {
                                dragStart = progress
                            } else {
                                dragStart = touchProgress
                                onSeek(touchProgress)
                            }
                        },
                        onDragEnd = { dragStart = null },
                        onDragCancel = { dragStart = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val base = dragStart ?: progress
                            val next = (base + dragAmount.x / width).coerceIn(0f, 1f)
                            dragStart = next
                            onSeek(next)
                        },
                    )
                },
        ) {
            val x = (size.width * progress.coerceIn(0f, 1f))
                .coerceIn(scrubberPx / 2f, size.width - scrubberPx / 2f)
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = scrubberPx / 2f,
                center = Offset(x, size.height / 2f + 0.5f),
            )
            drawCircle(
                color = ScrubberColor,
                radius = scrubberPx / 2f,
                center = Offset(x, size.height / 2f),
            )
        }
    }
}

private fun formatVoiceTime(seconds: Float): String {
    val total = seconds.coerceAtLeast(0f).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}
