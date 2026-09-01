package com.example.spectroom.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object PreferredAgeRangeLogic {
    const val ABSOLUTE_MIN = 18
    const val ABSOLUTE_MAX = 80
    const val MAX_SPAN = 12

    fun updateMin(currentMin: Int, currentMax: Int, proposed: Int): Pair<Int, Int> {
        val newMin = proposed.coerceIn(ABSOLUTE_MIN, ABSOLUTE_MAX)

        return if (currentMax - newMin > MAX_SPAN) {
            val newMax = newMin + MAX_SPAN
            if (newMax > ABSOLUTE_MAX) {
                max(ABSOLUTE_MIN, ABSOLUTE_MAX - MAX_SPAN) to ABSOLUTE_MAX
            } else {
                newMin to newMax
            }
        } else if (newMin > currentMax) {
            currentMax to currentMax
        } else {
            newMin to currentMax
        }
    }

    fun updateMax(currentMin: Int, currentMax: Int, proposed: Int): Pair<Int, Int> {
        val newMax = proposed.coerceIn(ABSOLUTE_MIN, ABSOLUTE_MAX)

        return if (newMax - currentMin > MAX_SPAN) {
            val newMin = newMax - MAX_SPAN
            if (newMin < ABSOLUTE_MIN) {
                ABSOLUTE_MIN to minOf(ABSOLUTE_MAX, ABSOLUTE_MIN + MAX_SPAN)
            } else {
                newMin to newMax
            }
        } else if (newMax < currentMin) {
            currentMin to currentMin
        } else {
            currentMin to newMax
        }
    }

    fun clampRange(min: Int, max: Int): Pair<Int, Int> {
        var newMin = min.coerceIn(ABSOLUTE_MIN, ABSOLUTE_MAX)
        var newMax = max.coerceIn(ABSOLUTE_MIN, ABSOLUTE_MAX)
        if (newMin > newMax) {
            val swapped = newMin
            newMin = newMax
            newMax = swapped
        }
        if (newMax - newMin > MAX_SPAN) {
            newMax = minOf(ABSOLUTE_MAX, newMin + MAX_SPAN)
        }
        return newMin to newMax
    }

    fun initialRange(userAge: Int, savedMin: Int?, savedMax: Int?): Pair<Int, Int> {
        val age = userAge.coerceIn(ABSOLUTE_MIN, ABSOLUTE_MAX)
        val hasSaved = (savedMin ?: 0) > 0 && (savedMax ?: 0) > 0
        val (min, max) = if (hasSaved) {
            savedMin!! to savedMax!!
        } else {
            age to minOf(age + 5, ABSOLUTE_MAX)
        }
        return clampRange(min, max)
    }
}

private enum class ActiveThumb { Min, Max }

@Composable
fun PreferredAgeRangeSlider(
    minAge: Int,
    maxAge: Int,
    onRangeChange: (min: Int, max: Int) -> Unit,
    modifier: Modifier = Modifier,
    absoluteMin: Int = PreferredAgeRangeLogic.ABSOLUTE_MIN,
    absoluteMax: Int = PreferredAgeRangeLogic.ABSOLUTE_MAX,
    accentColor: Color = TabSelected,
) {
    val colors = LocalSpectRoomColors.current
    val density = LocalDensity.current
    val thumbSize = 28.dp
    val thumbHitSlop = 16.dp
    val trackHeight = 10.dp
    val thumbRadiusPx = with(density) { (thumbSize / 2).toPx() }
    val hitRadiusPx = with(density) { (thumbSize / 2 + thumbHitSlop).toPx() }
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val thumbBorderPx = with(density) { 3.dp.toPx() }
    val thumbDotRadiusPx = with(density) { 5.5.dp.toPx() }
    val sliderHeight = thumbSize + thumbHitSlop * 2

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(sliderHeight),
    ) {
        val inset = thumbRadiusPx
        val usableWidth = with(density) {
            max(maxWidth.toPx() - thumbRadiusPx * 2f, 1f)
        }
        val midY = with(density) { maxHeight.toPx() / 2f }

        val minAgeState by rememberUpdatedState(minAge)
        val maxAgeState by rememberUpdatedState(maxAge)
        val onRangeChangeState by rememberUpdatedState(onRangeChange)

        var activeThumb by remember { mutableStateOf<ActiveThumb?>(null) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(absoluteMin, absoluteMax, usableWidth, inset, midY, hitRadiusPx) {
                    fun xForAgeState(age: Int): Float {
                        val progress = (age - absoluteMin).toFloat() / (absoluteMax - absoluteMin).coerceAtLeast(1)
                        return inset + progress * usableWidth
                    }

                    fun ageForX(x: Float): Int {
                        val relative = ((x - inset) / usableWidth).coerceIn(0f, 1f)
                        return (absoluteMin + relative * (absoluteMax - absoluteMin)).roundToInt()
                    }

                    fun thumbAt(x: Float, y: Float): ActiveThumb? {
                        val currentMinX = xForAgeState(minAgeState)
                        val currentMaxX = xForAgeState(maxAgeState)
                        val minDistance = hypotDistance(x, y, currentMinX, midY)
                        val maxDistance = hypotDistance(x, y, currentMaxX, midY)
                        return when {
                            minDistance <= hitRadiusPx && maxDistance <= hitRadiusPx -> {
                                if (minDistance <= maxDistance) ActiveThumb.Min else ActiveThumb.Max
                            }
                            minDistance <= hitRadiusPx -> ActiveThumb.Min
                            maxDistance <= hitRadiusPx -> ActiveThumb.Max
                            else -> null
                        }
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val thumb = thumbAt(down.position.x, down.position.y) ?: return@awaitEachGesture
                        activeThumb = thumb
                        down.consume()

                        drag(down.id) { change ->
                            change.consume()
                            val x = change.position.x
                            when (thumb) {
                                ActiveThumb.Min -> {
                                    val (newMin, newMax) = PreferredAgeRangeLogic.updateMin(
                                        minAgeState,
                                        maxAgeState,
                                        ageForX(x),
                                    )
                                    onRangeChangeState(newMin, newMax)
                                }
                                ActiveThumb.Max -> {
                                    val (newMin, newMax) = PreferredAgeRangeLogic.updateMax(
                                        minAgeState,
                                        maxAgeState,
                                        ageForX(x),
                                    )
                                    onRangeChangeState(newMin, newMax)
                                }
                            }
                        }
                        activeThumb = null
                    }
                },
        ) {
            val minX = inset + (minAge - absoluteMin).toFloat() /
                (absoluteMax - absoluteMin).coerceAtLeast(1) * usableWidth
            val maxX = inset + (maxAge - absoluteMin).toFloat() /
                (absoluteMax - absoluteMin).coerceAtLeast(1) * usableWidth
            val trackColor = colors.text.copy(alpha = 0.08f)
            val cardColor = colors.card

            val centerX = size.width / 2f

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(centerX - usableWidth / 2f, midY - trackHeightPx / 2f),
                size = Size(usableWidth, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f),
            )

            val fillWidth = max(maxX - minX, trackHeightPx)
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.45f), accentColor),
                    startX = minX,
                    endX = minX + fillWidth,
                ),
                topLeft = Offset(minX, midY - trackHeightPx / 2f),
                size = Size(fillWidth, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f),
            )

            drawThumb(
                center = Offset(minX, midY),
                thumbRadius = thumbRadiusPx,
                borderWidth = thumbBorderPx,
                dotRadius = thumbDotRadiusPx,
                fillColor = cardColor,
                accentColor = accentColor,
                isActive = activeThumb == ActiveThumb.Min,
            )
            drawThumb(
                center = Offset(maxX, midY),
                thumbRadius = thumbRadiusPx,
                borderWidth = thumbBorderPx,
                dotRadius = thumbDotRadiusPx,
                fillColor = cardColor,
                accentColor = accentColor,
                isActive = activeThumb == ActiveThumb.Max,
            )
        }
    }
}

private fun hypotDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}

private fun DrawScope.drawThumb(
    center: Offset,
    thumbRadius: Float,
    borderWidth: Float,
    dotRadius: Float,
    fillColor: Color,
    accentColor: Color,
    isActive: Boolean,
) {
    val shadowOffset = Offset(0f, 2.dp.toPx())
    drawCircle(
        color = Color.Black.copy(alpha = if (isActive) 0.22f else 0.18f),
        radius = thumbRadius + 1f,
        center = center + shadowOffset,
    )
    drawCircle(color = fillColor, radius = thumbRadius, center = center)
    drawCircle(
        color = accentColor,
        radius = thumbRadius,
        center = center,
        style = Stroke(width = borderWidth),
    )
    drawCircle(color = accentColor, radius = dotRadius, center = center)
}
