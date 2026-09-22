package com.example.spectroom.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.ChatImageLimits
import com.example.spectroom.data.model.SupportTopic
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun HelpSupportSheet(
    lockedTopic: SupportTopic? = null,
    isSending: Boolean,
    sendSucceeded: Boolean,
    errorMessage: String?,
    onSend: (topic: SupportTopic, message: String, imageUri: Uri?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    var selectedTopic by remember(lockedTopic) {
        mutableStateOf(lockedTopic ?: SupportTopic.SAFETY)
    }
    var message by remember { mutableStateOf("") }
    var didAttemptSend by remember { mutableStateOf(false) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageTooLargeAlert by remember { mutableStateOf(false) }
    val maxLength = 500
    val trimmed = message.trim()
    val showMinWarning = didAttemptSend && !sendSucceeded && trimmed.length < 20
    val topics = lockedTopic?.let { listOf(it) } ?: SupportTopic.selectableDefault

    val pickSupportImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (ChatImageLimits.validateOriginal(context, uri)) {
            ChatImageLimits.ValidationResult.TooLarge -> showImageTooLargeAlert = true
            ChatImageLimits.ValidationResult.Ok,
            ChatImageLimits.ValidationResult.Unavailable,
            -> attachedImageUri = uri
        }
    }

    if (showImageTooLargeAlert) {
        AlertDialog(
            onDismissRequest = { showImageTooLargeAlert = false },
            title = { Text(stringResource(R.string.messages_image_error_size_title)) },
            text = { Text(stringResource(R.string.messages_image_error_size)) },
            confirmButton = {
                TextButton(onClick = { showImageTooLargeAlert = false }) {
                    Text(stringResource(R.string.auth_ok))
                }
            },
        )
    }

    SettingsFullScreenSheet(onDismiss = onDismiss) {
        SettingsSheetColumn {
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
                        .align(Alignment.CenterEnd)
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
                                    attachedImageUri = null
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
                Text(
                    text = stringResource(R.string.support_attachment_label),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                val attached = attachedImageUri
                if (attached != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = attached,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.support_attachment_added),
                                color = colors.text,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.support_attachment_hint),
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                            )
                        }
                        if (!sendSucceeded) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.support_attachment_remove),
                                tint = colors.secondaryText,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { attachedImageUri = null },
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, TabSelected.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .clickable(enabled = !sendSucceeded && !isSending) {
                                pickSupportImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = TabSelected,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.padding(start = 10.dp))
                        Text(
                            text = stringResource(R.string.support_attachment_add),
                            color = TabSelected,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = TabSelected,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.support_attachment_hint),
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        didAttemptSend = true
                        if (trimmed.length < 20) return@Button
                        onSend(selectedTopic, trimmed, attachedImageUri)
                    },
                    enabled = !isSending && !sendSucceeded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sendSucceeded) LikeGreen else TabSelected,
                        disabledContainerColor = when {
                            sendSucceeded -> LikeGreen
                            isSending -> Color.Gray
                            else -> TabSelected.copy(alpha = 0.45f)
                        },
                        contentColor = Color.White,
                        disabledContentColor = Color.White,
                    ),
                ) {
                    when {
                        isSending -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                        )
                        sendSucceeded -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text(
                                text = stringResource(R.string.support_button_sent),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                        else -> Text(
                            text = stringResource(R.string.support_button_send),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
