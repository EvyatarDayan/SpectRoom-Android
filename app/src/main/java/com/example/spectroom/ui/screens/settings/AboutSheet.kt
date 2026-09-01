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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors

@Composable
fun AboutSheet(
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current

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
                    text = stringResource(R.string.about_title),
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
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.about_spectroom_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.about_intro_1),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.about_intro_2),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = colors.text,
                )

                Spacer(modifier = Modifier.height(24.dp))
                AboutSection(
                    title = stringResource(R.string.about_focus_title),
                    body = stringResource(R.string.about_focus_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                AboutSection(
                    title = stringResource(R.string.about_mission_title),
                    body = stringResource(R.string.about_mission_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                AboutSection(
                    title = stringResource(R.string.about_values_title),
                    body = stringResource(R.string.about_values_body),
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.border)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.about_abashelari_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.about_abashelari_1),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.about_abashelari_2),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = colors.text,
                )

                Spacer(modifier = Modifier.height(24.dp))
                AboutSection(
                    title = stringResource(R.string.about_studio_title),
                    body = stringResource(R.string.about_studio_body),
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.about_closing),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = colors.text,
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun AboutSection(
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
