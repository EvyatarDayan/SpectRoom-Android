package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.UserReport
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportedProfilesSheet(
    reports: List<UserReport>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var selected by remember { mutableStateOf<UserReport?>(null) }

    SettingsFullScreenSheet(onDismiss = onDismiss) {
            Column(modifier = Modifier.settingsSheetSurface()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_reported_profiles_modal_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 72.dp)
                            .align(Alignment.Center),
                    )
                    Text(
                        text = stringResource(R.string.settings_done),
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
                }

                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = TabSelected)
                    }
                    reports.isEmpty() -> ReportedProfilesEmptyState()
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        items(reports, key = { it.id }) { report ->
                            ReportedProfileRow(
                                report = report,
                                onClick = { selected = report },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }

            selected?.let { report ->
                ReportDetailsOverlay(
                    report = report,
                    onDismiss = { selected = null },
                )
            }
    }
}

@Composable
private fun ReportedProfilesEmptyState() {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(12.dp, CircleShape, spotColor = Color(0xFFFF9800).copy(alpha = 0.14f))
                .background(Color(0xFFFF9800).copy(alpha = 0.12f), CircleShape)
                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🚩", fontSize = 48.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_reported_profiles_empty),
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_reported_profiles_empty_description),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReportedProfileRow(
    report: UserReport,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val dateText = report.createdAt?.let {
        stringResource(
            R.string.settings_reported_profiles_since,
            formatReportedDate(it),
        )
    } ?: stringResource(R.string.settings_reported_profiles_no_date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReportedProfileAvatar(
            name = report.reportedName,
            photoUrl = report.primaryPhotoUrl,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.reportedName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = colors.text,
            )
            Text(
                text = dateText,
                color = colors.secondaryText,
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ReportedProfileAvatar(
    name: String,
    photoUrl: String?,
) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TabSelected.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = TabSelected,
            )
        }
    }
}

@Composable
private fun ReportDetailsOverlay(
    report: UserReport,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val statusLabel = when (report.status.lowercase()) {
        "reviewed" -> stringResource(R.string.settings_reported_profiles_status_reviewed)
        "dismissed" -> stringResource(R.string.settings_reported_profiles_status_dismissed)
        "actioned" -> stringResource(R.string.settings_reported_profiles_status_actioned)
        else -> stringResource(R.string.settings_reported_profiles_status_pending)
    }
    val headline = report.createdAt?.let {
        stringResource(
            R.string.settings_reported_profiles_detail_headline,
            report.reportedName,
            formatReportedDetailDate(it),
        )
    } ?: stringResource(
        R.string.settings_reported_profiles_detail_headline_no_date,
        report.reportedName,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                ReportedProfileAvatar(
                    name = report.reportedName,
                    photoUrl = report.primaryPhotoUrl,
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = headline,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            DetailRow(
                label = stringResource(R.string.settings_reported_profiles_detail_category),
                value = report.category,
            )
            report.details?.trim()?.takeIf { it.isNotEmpty() }?.let { details ->
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow(
                    label = stringResource(R.string.settings_reported_profiles_detail_details),
                    value = details,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(
                label = stringResource(R.string.settings_reported_profiles_detail_status),
                value = statusLabel,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.common_got_it),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(colors.background)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    val colors = LocalSpectRoomColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = colors.secondaryText,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = colors.text,
        )
    }
}

private fun formatReportedDate(date: Date): String =
    SimpleDateFormat("d MMM yyyy 'at' HH:mm", Locale.getDefault()).format(date)

private fun formatReportedDetailDate(date: Date): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(date)
