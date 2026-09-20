package com.example.spectroom.ui.screens.messages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.data.model.MessageDisplay
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Long-press menu: reactions + Copy + Report + Delete (iOS MessageQuickActionsOverlay). */
@Composable
fun MessageQuickActionsOverlay(
    canCopy: Boolean,
    onReact: (String) -> Unit,
    onCopy: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val scope = rememberCoroutineScope()
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
        label = "quickActionsScale",
    )
    val opacity by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = if (appeared) 220 else 160),
        label = "quickActionsOpacity",
    )

    fun dismissThen(action: () -> Unit) {
        scope.launch {
            appeared = false
            delay(160)
            action()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(40f)
            .background(Color.Black.copy(alpha = 0.28f * opacity))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissThen(onCancel) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(260.dp)
                .scale(scale)
                .graphicsLayer { alpha = opacity }
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card.copy(alpha = 0.96f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MessageDisplay.QUICK_REACTION_EMOJIS.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { dismissThen { onReact(emoji) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, fontSize = 28.sp)
                    }
                }
            }
            HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f))
            QuickActionRow(
                title = stringResource(R.string.messages_copy_menu),
                icon = Icons.Outlined.ContentCopy,
                enabled = canCopy,
                destructive = false,
                onClick = { dismissThen(onCopy) },
            )
            HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f))
            QuickActionRow(
                title = stringResource(R.string.messages_report_menu),
                icon = Icons.Outlined.Report,
                enabled = true,
                destructive = false,
                onClick = { dismissThen(onReport) },
            )
            HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f))
            QuickActionRow(
                title = stringResource(R.string.messages_delete_menu),
                icon = Icons.Outlined.Delete,
                enabled = true,
                destructive = true,
                onClick = { dismissThen(onDelete) },
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    destructive: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val tint = when {
        !enabled -> colors.secondaryText.copy(alpha = 0.45f)
        destructive -> Color.Red
        else -> colors.text
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = tint, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
