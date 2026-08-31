package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors

@Composable
fun PrivacySafetySheet(
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current

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
                    text = stringResource(R.string.privacy_safety_title),
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
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.privacy_safety_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.privacy_safety_intro),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = colors.text,
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_1_title),
                    body = stringResource(R.string.privacy_safety_section_1_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_2_title),
                    body = stringResource(R.string.privacy_safety_section_2_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_3_title),
                    body = stringResource(R.string.privacy_safety_section_3_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_4_title),
                    body = stringResource(R.string.privacy_safety_section_4_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_5_title),
                    body = stringResource(R.string.privacy_safety_section_5_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_6_title),
                    body = stringResource(R.string.privacy_safety_section_6_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_7_title),
                    body = stringResource(R.string.privacy_safety_section_7_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_8_title),
                    body = stringResource(R.string.privacy_safety_section_8_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrivacySection(
                    title = stringResource(R.string.privacy_safety_section_9_title),
                    body = stringResource(R.string.privacy_safety_section_9_body),
                )

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    body: String,
) {
    val colors = LocalSpectRoomColors.current
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.text,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = colors.text,
        )
    }
}
