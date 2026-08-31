package com.example.spectroom.ui.screens.messages

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** WhatsApp-style chat bubble with a small tail on the bottom corner (iOS parity). */
class ChatBubbleShape(
    private val isOutgoing: Boolean,
    private val cornerRadius: Float = 14f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height
        val r = cornerRadius
        val tail = 6f

        if (isOutgoing) {
            path.moveTo(r, 0f)
            path.lineTo(width - r - tail, 0f)
            path.arcTo(
                rect = Rect(width - r - tail - r, 0f, width - r - tail + r, r * 2f),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            path.lineTo(width - tail, height - r)
            path.arcTo(
                rect = Rect(width - r - tail - r, height - r * 2f, width - r - tail + r, height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 35f,
                forceMoveTo = false,
            )
            path.quadraticTo(width - tail * 0.2f, height, width, height)
            path.quadraticTo(width - 1f, height, width - tail - 8f, height)
            path.lineTo(r, height)
            path.arcTo(
                rect = Rect(0f, height - r * 2f, r * 2f, height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            path.lineTo(0f, r)
            path.arcTo(
                rect = Rect(0f, 0f, r * 2f, r * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
        } else {
            path.moveTo(r + tail, 0f)
            path.lineTo(width - r, 0f)
            path.arcTo(
                rect = Rect(width - r * 2f, 0f, width, r * 2f),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            path.lineTo(width, height - r)
            path.arcTo(
                rect = Rect(width - r * 2f, height - r * 2f, width, height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            path.lineTo(tail + 8f, height)
            path.quadraticTo(1f, height, 0f, height)
            path.quadraticTo(tail * 0.2f, height, tail, height - r + 4f)
            path.lineTo(tail, r)
            path.arcTo(
                rect = Rect(tail, 0f, tail + r * 2f, r * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
        }

        path.close()
        return Outline.Generic(path)
    }
}
