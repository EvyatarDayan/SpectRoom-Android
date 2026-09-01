package com.example.spectroom.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun BlockedUserAlertOverlay(
    firstName: String,
    onDismiss: (dontShowAgain: Boolean) -> Unit,
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { onDismiss(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LikeRed, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.FrontHand,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.messages_blocked_alert_title, firstName),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.messages_blocked_alert_detail),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dontShowAgain = !dontShowAgain },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (dontShowAgain) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.messages_blocked_alert_dont_show_again),
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onDismiss(dontShowAgain) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LikeRed,
                    ),
                ) {
                    Text(stringResource(R.string.common_ok), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun NewMessageNotificationModal(
    senderName: String,
    senderPhotoUrl: String?,
    onOpen: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TabSelected.copy(alpha = 0.1f))
                .clickable(onClick = onLater)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(colors.card, RoundedCornerShape(18.dp))
                    .clickable(enabled = false) {}
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = senderPhotoUrl,
                    contentDescription = senderName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryText.copy(alpha = 0.25f)),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.messages_new_message),
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = colors.text,
                )
                Text(
                    stringResource(R.string.messages_new_message_from, senderName),
                    color = colors.secondaryText,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(stringResource(R.string.messages_open_message), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onLater,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondaryText),
                ) {
                    Text(stringResource(R.string.messages_later), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun VoiceLongPressTipOverlay(onDismiss: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    val dismissInteraction = remember { MutableInteractionSource() }
    val okInteraction = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(28.dp)
    val cardBackground = if (colors.isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.82f)
    }
    val buttonBackground = if (colors.isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(cardBackground)
                    .border(
                        width = 0.5.dp,
                        color = if (colors.isDark) {
                            Color.White.copy(alpha = 0.14f)
                        } else {
                            Color.White.copy(alpha = 0.65f)
                        },
                        shape = cardShape,
                    )
                    .clickable(enabled = false) {}
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.messages_voice_long_press_tip_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.messages_voice_long_press_tip_message),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(buttonBackground)
                        .clickable(
                            interactionSource = okInteraction,
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.messages_voice_long_press_tip_got_it),
                        color = colors.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    )
                }
            }
        }
    }
}
