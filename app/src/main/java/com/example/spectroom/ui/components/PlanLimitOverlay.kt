package com.example.spectroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

/**
 * Reusable plan-restriction dialog (mirrors iOS `MediaDailyLimitOverlay`).
 */
@Composable
fun PlanLimitOverlay(
    title: String,
    message: String,
    showsPlansButton: Boolean = true,
    dismissButtonTitle: String = stringResource(R.string.messages_media_limit_not_now),
    plansButtonTitle: String = stringResource(R.string.messages_media_limit_view_plans),
    onDismiss: () -> Unit,
    onViewPlans: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val isDark = colors.isDark
    val scrim = Color.Black.copy(alpha = if (isDark) 0.55f else 0.28f)
    val panel = if (isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.95f)
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val primary = if (isDark) Color.White else Color.Black
    val secondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.55f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(0.18f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(panel)
                    .clickable(enabled = false) {}
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = secondary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = dismissButtonTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF8E8E93).copy(alpha = 0.28f))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 13.dp),
                    )
                    if (showsPlansButton) {
                        Text(
                            text = plansButtonTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(TabSelected)
                                .clickable(onClick = onViewPlans)
                                .padding(vertical = 13.dp),
                        )
                    }
                }
            }
        }
    }
}
