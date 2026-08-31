package com.example.spectroom.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    val xFraction: Float,
    val startYFraction: Float,
    val size: Float,
    val color: Color,
    val rotation: Float,
    val drift: Float,
)

@Composable
fun ConfettiBurst(
    modifier: Modifier = Modifier,
    particleCount: Int = 48,
) {
    val particles = remember {
        val palette = listOf(
            Color(0xFFE91E63),
            Color(0xFF2196F3),
            Color(0xFFFFC107),
            Color(0xFF4CAF50),
            Color(0xFFFF5722),
            Color(0xFF9C27B0),
        )
        List(particleCount) {
            ConfettiParticle(
                xFraction = Random.nextFloat(),
                startYFraction = Random.nextFloat() * 0.35f,
                size = 6f + Random.nextFloat() * 8f,
                color = palette.random(),
                rotation = Random.nextFloat() * 360f,
                drift = -40f + Random.nextFloat() * 80f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 2200, easing = LinearEasing))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        particles.forEach { particle ->
            val x = particle.xFraction * size.width + particle.drift * t
            val y = particle.startYFraction * size.height + t * size.height * 0.85f
            val alpha = (1f - t).coerceIn(0f, 1f)
            rotate(degrees = particle.rotation + t * 180f, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(x - particle.size / 2f, y - particle.size / 2f),
                    size = androidx.compose.ui.geometry.Size(particle.size, particle.size * 0.55f),
                )
            }
        }
    }
}
