package com.example.spectroom.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R
import com.example.spectroom.data.model.SupportTopic
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

private const val SupportEmail = "spectroom.support@gmail.com"

@Composable
fun HelpSupportSheet(
    lockedTopic: SupportTopic? = null,
    isSending: Boolean,
    sendSucceeded: Boolean,
    errorMessage: String?,
    onSend: (topic: SupportTopic, message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    var selectedTopic by remember(lockedTopic) {
        mutableStateOf(lockedTopic ?: SupportTopic.SAFETY)
    }
    var message by remember { mutableStateOf("") }
    var didAttemptSend by remember { mutableStateOf(false) }
    val maxLength = 500
    val trimmed = message.trim()
    val showMinWarning = didAttemptSend && !sendSucceeded && trimmed.length < 20
    val topics = lockedTopic?.let { listOf(it) } ?: SupportTopic.selectableDefault

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.common_close),
                    color = colors.text,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, colors.border, RoundedCornerShape(50))
                        .background(colors.card)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
                Text(
                    text = stringResource(R.string.support_nav_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 72.dp)
                        .align(Alignment.Center),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.support_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.support_subtitle),
                        color = colors.text.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.support_topic_label),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    topics.forEach { topic ->
                        val selected = topic == selectedTopic
                        Text(
                            text = stringResource(topic.labelRes),
                            color = if (selected) Color.White else colors.text,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) TabSelected else colors.card)
                                .border(
                                    1.dp,
                                    if (selected) Color.Transparent else colors.border,
                                    RoundedCornerShape(50),
                                )
                                .clickable(enabled = lockedTopic == null && !sendSucceeded) {
                                    selectedTopic = topic
                                    didAttemptSend = false
                                    message = ""
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.support_message_label),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= maxLength) message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp),
                    placeholder = {
                        Text(
                            stringResource(R.string.support_message_placeholder),
                            color = colors.secondaryText.copy(alpha = 0.6f),
                        )
                    },
                    enabled = !sendSucceeded,
                    shape = RoundedCornerShape(12.dp),
                    isError = showMinWarning,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.card,
                        unfocusedContainerColor = colors.card,
                        disabledContainerColor = colors.card.copy(alpha = 0.5f),
                        focusedBorderColor = if (showMinWarning) LikeRed else Color.Gray.copy(alpha = 0.25f),
                        unfocusedBorderColor = if (showMinWarning) LikeRed else Color.Gray.copy(alpha = 0.25f),
                        errorBorderColor = LikeRed,
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (showMinWarning) {
                            stringResource(R.string.support_message_min_chars)
                        } else {
                            " "
                        },
                        color = LikeRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${message.length} / $maxLength",
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                    )
                }
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = LikeRed, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        didAttemptSend = true
                        if (trimmed.length < 20) return@Button
                        onSend(selectedTopic, trimmed)
                    },
                    enabled = !isSending && !sendSucceeded,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sendSucceeded) Color(0xFF2E7D32) else TabSelected,
                    ),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(22.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (sendSucceeded) {
                                    R.string.support_button_sent
                                } else {
                                    R.string.support_button_send
                                },
                            ),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                    )
                    Text(
                        text = stringResource(R.string.support_or),
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .background(colors.card)
                        .clickable {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("mailto:$SupportEmail"),
                                ),
                            )
                        }
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "✉️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(
                            text = stringResource(R.string.support_email_label),
                            color = colors.text,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.height(20.dp),
                        )
                    }
                    Text(
                        text = SupportEmail,
                        color = TabSelected,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 30.dp, top = 6.dp),
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
