package com.example.spectroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.data.model.ModerationNotice
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

@Composable
fun SystemWarningOverlay(
    notice: ModerationNotice,
    onDismiss: () -> Unit,
    onContactSupport: (() -> Unit)? = null,
) {
    val colors = LocalSpectRoomColors.current
    val title = when (notice) {
        is ModerationNotice.Warning -> stringResource(R.string.admin_warning_popup_title)
        is ModerationNotice.FreezeAcknowledgement,
        is ModerationNotice.LoginBlocked,
        -> stringResource(R.string.admin_freeze_popup_title)
        is ModerationNotice.BanAcknowledgement,
        is ModerationNotice.LoginBanned,
        -> stringResource(R.string.admin_ban_popup_title)
        is ModerationNotice.BanLifted -> stringResource(R.string.admin_ban_lifted_popup_title)
        is ModerationNotice.FreezeLifted -> stringResource(R.string.admin_freeze_lifted_popup_title)
        is ModerationNotice.DeletedAcknowledgement,
        is ModerationNotice.LoginDeleted,
        -> stringResource(R.string.admin_deleted_popup_title)
    }
    val message = when (notice) {
        is ModerationNotice.Warning -> notice.message
        is ModerationNotice.FreezeAcknowledgement -> stringResource(
            R.string.admin_freeze_popup_message,
            notice.hours,
            formatUnlock(notice.until),
        )
        is ModerationNotice.LoginBlocked -> {
            stringResource(R.string.admin_freeze_popup_message, notice.hours, formatUnlock(notice.until))
        }
        is ModerationNotice.BanAcknowledgement,
        is ModerationNotice.LoginBanned,
        -> stringResource(R.string.admin_ban_popup_message)
        is ModerationNotice.BanLifted -> stringResource(R.string.admin_ban_lifted_popup_message)
        is ModerationNotice.FreezeLifted -> stringResource(R.string.admin_freeze_lifted_popup_message)
        is ModerationNotice.DeletedAcknowledgement,
        is ModerationNotice.LoginDeleted,
        -> stringResource(R.string.admin_deleted_popup_message)
    }
    val accept = when (notice) {
        is ModerationNotice.Warning -> stringResource(R.string.admin_warning_popup_accept)
        is ModerationNotice.FreezeAcknowledgement,
        is ModerationNotice.LoginBlocked,
        -> stringResource(R.string.admin_freeze_popup_accept)
        is ModerationNotice.BanAcknowledgement,
        is ModerationNotice.LoginBanned,
        -> stringResource(R.string.admin_ban_popup_accept)
        is ModerationNotice.BanLifted -> stringResource(R.string.admin_ban_lifted_popup_accept)
        is ModerationNotice.FreezeLifted -> stringResource(R.string.admin_freeze_lifted_popup_accept)
        is ModerationNotice.DeletedAcknowledgement,
        is ModerationNotice.LoginDeleted,
        -> stringResource(R.string.admin_deleted_popup_accept)
    }
    val showSupport = when (notice) {
        is ModerationNotice.FreezeAcknowledgement,
        is ModerationNotice.LoginBlocked,
        is ModerationNotice.BanAcknowledgement,
        is ModerationNotice.LoginBanned,
        is ModerationNotice.DeletedAcknowledgement,
        is ModerationNotice.LoginDeleted,
        -> true
        else -> false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            ) {
                Text(accept, fontWeight = FontWeight.SemiBold)
            }
            if (showSupport && onContactSupport != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onContactSupport) {
                    Text(
                        stringResource(R.string.admin_freeze_popup_contact_support),
                        color = TabSelectedBlue,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private val TabSelectedBlue = Color(0xFF007AFF)

/** Matches iOS `ModerationNotice.formattedUnlockDate` (abbreviated month + day + time). */
private fun formatUnlock(date: Date): String {
    val zoned = date.toInstant().atZone(ZoneId.systemDefault())
    val locale = Locale.getDefault()
    val datePart = DateTimeFormatter.ofPattern("MMM d", locale).format(zoned)
    val timePart = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(zoned)
    return "$datePart, $timePart"
}
