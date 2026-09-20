package com.example.spectroom.ui.screens.messages

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.data.model.MessageDeleteOption
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

/** Centered alert: Delete for me / Delete for everyone (iOS MessageDeleteOptionsOverlay). */
@Composable
fun MessageDeleteOptionsOverlay(
    allowsDeleteForEveryone: Boolean,
    onDelete: (MessageDeleteOption) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var selected by remember { mutableStateOf(MessageDeleteOption.FOR_ME) }
    if (!allowsDeleteForEveryone && selected == MessageDeleteOption.FOR_EVERYONE) {
        selected = MessageDeleteOption.FOR_ME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(41f)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    text = stringResource(R.string.messages_delete_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                MessageDeleteOption.entries.forEach { option ->
                    val enabled = option != MessageDeleteOption.FOR_EVERYONE || allowsDeleteForEveryone
                    val title = stringResource(
                        when (option) {
                            MessageDeleteOption.FOR_ME -> R.string.messages_delete_option_for_me
                            MessageDeleteOption.FOR_EVERYONE -> R.string.messages_delete_option_for_everyone
                        },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { selected = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (selected == option) {
                                Icons.Filled.RadioButtonChecked
                            } else {
                                Icons.Filled.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = when {
                                !enabled -> colors.secondaryText.copy(alpha = 0.45f)
                                selected == option -> TabSelected
                                else -> colors.secondaryText
                            },
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            color = if (enabled) colors.text else colors.secondaryText.copy(alpha = 0.45f),
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    color = colors.text,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onCancel)
                        .padding(vertical = 14.dp),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .align(Alignment.CenterVertically)
                        .background(colors.secondaryText.copy(alpha = 0.2f)),
                )
                Text(
                    text = stringResource(R.string.messages_delete_confirm),
                    color = Color.Red,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val choice = if (
                                selected == MessageDeleteOption.FOR_EVERYONE && !allowsDeleteForEveryone
                            ) {
                                MessageDeleteOption.FOR_ME
                            } else {
                                selected
                            }
                            onDelete(choice)
                        }
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}

/**
 * Chat report first step — matches iOS `ReportConfirmationOverlay`
 * (light scrim, alert action bar, top-trailing close).
 */
@Composable
fun ChatReportConfirmationOverlay(
    firstName: String,
    onCancel: () -> Unit,
    onReport: () -> Unit,
    onReportAndBlock: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(42f)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.profile_report_confirm_title, firstName),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.profile_report_confirm_message, firstName),
                        fontSize = 14.sp,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center,
                    )
                }
                HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.profile_report_confirm_report_and_block),
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onReportAndBlock)
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .align(Alignment.CenterVertically)
                            .background(colors.secondaryText.copy(alpha = 0.2f)),
                    )
                    Text(
                        text = stringResource(R.string.profile_report_confirm_action),
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onReport)
                            .padding(vertical = 14.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.background)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_cancel),
                    tint = colors.secondaryText,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
