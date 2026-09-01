package com.example.spectroom.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Photo
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.data.model.ProfileReportReason
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

private const val SOMETHING_ELSE_MIN_CHARS = 20
private const val SOMETHING_ELSE_MAX_CHARS = 200

@Composable
fun ReportReasonOverlay(
    firstName: String,
    onCancel: () -> Unit,
    onSubmit: (ProfileReportReason, String?) -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var selectedReason by remember { mutableStateOf(ProfileReportReason.INAPPROPRIATE_CONTENT) }
    var somethingElseText by remember { mutableStateOf("") }
    var didAttemptSubmit by remember { mutableStateOf(false) }
    var showBlockedNotReported by remember { mutableStateOf(false) }

    val trimmedSomethingElse = somethingElseText.trim()
    val isSomethingElseActive = selectedReason == ProfileReportReason.SOMETHING_ELSE
    val showSomethingElseMinWarning = isSomethingElseActive &&
        didAttemptSubmit &&
        trimmedSomethingElse.length < SOMETHING_ELSE_MIN_CHARS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .zIndex(35f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (showBlockedNotReported) onCancel()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showBlockedNotReported) {
            BlockedNotReportedCard(
                firstName = firstName,
                onGotIt = onCancel,
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.card)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_report_blocked_notice, firstName),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = LikeRed,
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.profile_report_title, firstName),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.profile_report_confidential, firstName),
                    fontSize = 15.sp,
                    color = colors.secondaryText,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProfileReportReason.entries.forEach { reason ->
                    ReportReasonOption(
                        reason = reason,
                        isSelected = selectedReason == reason,
                        onClick = {
                            selectedReason = reason
                            didAttemptSubmit = false
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                SomethingElseField(
                    text = somethingElseText,
                    onTextChange = { newValue ->
                        somethingElseText = newValue.take(SOMETHING_ELSE_MAX_CHARS)
                    },
                    isActive = isSomethingElseActive,
                    showMinWarning = showSomethingElseMinWarning,
                    onActivate = {
                        selectedReason = ProfileReportReason.SOMETHING_ELSE
                        didAttemptSubmit = false
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(1.dp, colors.secondaryText.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { showBlockedNotReported = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.common_cancel),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.text,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LikeRed.copy(alpha = 0.1f))
                            .border(1.dp, LikeRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                didAttemptSubmit = true
                                if (selectedReason == ProfileReportReason.SOMETHING_ELSE) {
                                    if (trimmedSomethingElse.length >= SOMETHING_ELSE_MIN_CHARS) {
                                        onSubmit(selectedReason, trimmedSomethingElse)
                                    }
                                } else {
                                    onSubmit(selectedReason, null)
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_report_submit),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = LikeRed,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportThankYouOverlay(
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .zIndex(35f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
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
                )
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.profile_report_thank_you),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .widthIn(min = 140.dp, max = 200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TabSelected.copy(alpha = 0.1f))
                    .border(1.dp, TabSelected.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.common_ok),
                    color = TabSelected,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BlockedNotReportedCard(
    firstName: String,
    onGotIt: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.profile_report_blocked_not_reported, firstName),
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
        )
        HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.2f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onGotIt)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.common_got_it),
                color = TabSelected,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun ReportReasonOption(
    reason: ProfileReportReason,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val borderColor = if (isSelected) TabSelected else colors.secondaryText.copy(alpha = 0.2f)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) TabSelected.copy(alpha = 0.08f) else colors.background)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReportReasonIcon(reason = reason)
        Text(
            text = stringResource(reason.labelRes),
            fontSize = 16.sp,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = TabSelected,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ReportReasonIcon(reason: ProfileReportReason) {
    when (reason) {
        ProfileReportReason.INAPPROPRIATE_CONTENT -> {
            Icon(
                imageVector = Icons.Outlined.Photo,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp),
            )
        }
        ProfileReportReason.FAKE_PROFILE_OR_SPAM -> {
            Text(text = "🤖", fontSize = 20.sp)
        }
        ProfileReportReason.HARASSMENT_OR_BULLYING -> {
            Text(text = "🚫", fontSize = 20.sp)
        }
        ProfileReportReason.SOMETHING_ELSE -> {
            Text(text = "💬", fontSize = 20.sp)
        }
    }
}

@Composable
private fun SomethingElseField(
    text: String,
    onTextChange: (String) -> Unit,
    isActive: Boolean,
    showMinWarning: Boolean,
    onActivate: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val fieldAlpha = if (isActive) 1f else 0.45f
    val borderColor = when {
        showMinWarning -> LikeRed
        isActive -> colors.secondaryText.copy(alpha = 0.25f)
        else -> colors.secondaryText.copy(alpha = 0.15f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isActive) Modifier else Modifier),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background.copy(alpha = if (isActive) 1f else 0.6f))
                .border(
                    width = if (showMinWarning) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onActivate)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.profile_report_something_else_placeholder),
                    color = Color.Gray.copy(alpha = if (isActive) 0.6f else 0.35f),
                    fontSize = 16.sp,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = isActive,
                textStyle = TextStyle(
                    color = colors.text.copy(alpha = fieldAlpha),
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(TabSelected),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (showMinWarning) {
                    stringResource(R.string.support_message_min_chars)
                } else {
                    " "
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LikeRed,
            )
            Text(
                text = "${text.length} / $SOMETHING_ELSE_MAX_CHARS",
                fontSize = 12.sp,
                color = colors.secondaryText.copy(alpha = fieldAlpha),
            )
        }
    }
}
