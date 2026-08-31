package com.example.spectroom.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.spectroom.ui.theme.LocalSpectRoomColors

@Composable
fun MatchDetailsOverlay(
    firstName: String,
    breakdown: MatchBreakdown?,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val percentage = breakdown?.percentage ?: 0
    val matchColor = if (percentage > 49) Color(0xFF4CAF50) else colors.text.copy(alpha = 0.75f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
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
            Spacer(modifier = Modifier.height(8.dp))
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

            DetailRow(
                title = stringResource(R.string.search_match_details_age),
                value = ageDetailText(firstName, breakdown?.ageDeltaFromYou ?: 0),
            )
            if (!breakdown?.commonLookingFor.isNullOrEmpty()) {
                DetailRow(
                    title = stringResource(R.string.search_match_details_looking_for),
                    value = breakdown!!.commonLookingFor.joinToString(),
                )
            }
            if (!breakdown?.commonInterests.isNullOrEmpty()) {
                DetailRow(
                    title = stringResource(R.string.search_match_details_interests),
                    value = breakdown!!.commonInterests.joinToString(),
                )
            } else {
                DetailRow(
                    title = stringResource(R.string.search_match_details_interests),
                    value = stringResource(R.string.search_match_details_interests_none),
                )
            }
            if (!breakdown?.commonSensoryPreferences.isNullOrEmpty()) {
                DetailRow(
                    title = stringResource(R.string.search_match_details_sensory),
                    value = breakdown!!.commonSensoryPreferences.joinToString(),
                )
            }
            if (!breakdown?.sharedLifestyle.isNullOrBlank()) {
                DetailRow(
                    title = stringResource(R.string.search_match_details_lifestyle),
                    value = breakdown!!.sharedLifestyle.orEmpty(),
                )
            }
            if (!breakdown?.commonIdealFirstDates.isNullOrEmpty()) {
                DetailRow(
                    title = stringResource(R.string.search_match_details_ideal_first_date),
                    value = breakdown!!.commonIdealFirstDates.joinToString(),
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
