package com.example.spectroom.ui.screens.discover

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.data.model.MatchBreakdown
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun MatchDetailsOverlay(
    firstName: String,
    breakdown: MatchBreakdown?,
    showsFullBreakdown: Boolean,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val percentage = breakdown?.percentage ?: 0
    val matchColor = if (percentage > 49) {
        LikeGreen
    } else {
        colors.text.copy(alpha = 0.75f)
    }
    val gotItBackground = if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val scrimAlpha = if (colors.isDark) 0.6f else 0.35f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .zIndex(40f)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(enabled = false) {}
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.search_match_details_title, firstName),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.search_match_details_percent,
                    percentage,
                ),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = matchColor,
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (showsFullBreakdown) {
                DetailRow(
                    title = stringResource(R.string.search_match_details_age),
                    value = ageDetailText(firstName, breakdown?.ageDeltaFromYou ?: 0),
                )
                val lookingFor = breakdown?.commonLookingFor.orEmpty()
                if (lookingFor.isNotEmpty()) {
                    DetailRow(
                        title = stringResource(R.string.search_match_details_looking_for),
                        value = lookingFor.joinToString(),
                    )
                }
                val interests = breakdown?.commonInterests.orEmpty()
                if (interests.isNotEmpty()) {
                    DetailRow(
                        title = stringResource(R.string.search_match_details_interests),
                        value = interests.joinToString(),
                    )
                }
                val sensory = breakdown?.commonSensoryPreferences.orEmpty()
                if (sensory.isNotEmpty()) {
                    DetailRow(
                        title = stringResource(R.string.search_match_details_sensory),
                        value = sensory.joinToString(),
                    )
                }
                val lifestyle = breakdown?.sharedLifestyle.orEmpty()
                if (lifestyle.isNotBlank()) {
                    DetailRow(
                        title = stringResource(R.string.search_match_details_lifestyle),
                        value = lifestyle,
                    )
                }
                val idealFirstDate = breakdown?.commonIdealFirstDates.orEmpty()
                if (idealFirstDate.isNotEmpty()) {
                    DetailRow(
                        title = stringResource(R.string.search_match_details_ideal_first_date),
                        value = idealFirstDate.joinToString(),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.search_match_details_upgrade_message),
                    fontSize = 15.sp,
                    color = colors.secondaryText,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (showsFullBreakdown) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(gotItBackground),
                ) {
                    Text(
                        text = stringResource(R.string.common_got_it),
                        color = colors.text,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(gotItBackground),
                    ) {
                        Text(
                            text = stringResource(R.string.search_match_details_not_now),
                            color = colors.text,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    TextButton(
                        onClick = onUpgrade,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(TabSelected),
                    ) {
                        Text(
                            text = stringResource(R.string.search_match_details_view_plans),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LookingForDetailsOverlay(
    items: List<String>,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .zIndex(40f)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(enabled = false) {}
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_looking_for_details_title),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(12.dp))
            items.forEach { item ->
                Text(
                    text = "• $item",
                    color = colors.text,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.background),
            ) {
                Text(stringResource(R.string.common_got_it), color = colors.text)
            }
        }
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    val colors = LocalSpectRoomColors.current
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, color = colors.secondaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ageDetailText(firstName: String, delta: Int): String = when {
    delta == 0 -> stringResource(R.string.search_match_details_age_same, firstName)
    delta < 0 -> stringResource(R.string.search_match_details_age_younger, -delta, firstName)
    else -> stringResource(R.string.search_match_details_age_older, delta, firstName)
}
