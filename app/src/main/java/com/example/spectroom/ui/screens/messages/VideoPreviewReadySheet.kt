package com.example.spectroom.ui.screens.messages

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPreviewReadySheet(
    thumbnailModel: Any?,
    durationSeconds: Double,
    isSending: Boolean,
    mediaRemaining: Int? = null,
    onUpgrade: (() -> Unit)? = null,
    onSend: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val durationText = formatVideoDuration(durationSeconds)
    ModalBottomSheet(
        onDismissRequest = { if (!isSending) onDiscard() },
        sheetState = sheetState,
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.messages_video_ready),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = thumbnailModel,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp),
                    )
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(6.dp),
                    )
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDiscard,
                    enabled = !isSending,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondaryText.copy(alpha = 0.12f),
                        contentColor = TabSelected,
                        disabledContainerColor = colors.secondaryText.copy(alpha = 0.08f),
                        disabledContentColor = TabSelected.copy(alpha = 0.45f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onSend,
                    enabled = !isSending,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LikeGreen),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(R.string.messages_sending),
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.messages_send),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            if (mediaRemaining != null) {
                MediaRemainingUpgradeFooter(
                    remainingCount = mediaRemaining,
                    enabled = !isSending,
                    onUpgrade = onUpgrade,
                )
            }
        }
    }
}

internal fun formatVideoDuration(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}
