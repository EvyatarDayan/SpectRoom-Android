package com.example.spectroom.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.EnergyLevel
import com.example.spectroom.data.model.IdealFirstDateOption
import com.example.spectroom.data.model.MatchBreakdown
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.data.model.SensoryPreferenceOption
import com.example.spectroom.ui.theme.DistanceAccent
import com.example.spectroom.ui.theme.EnergyFullySocial
import com.example.spectroom.ui.theme.EnergyLow
import com.example.spectroom.ui.theme.EnergyModerate
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileModal(
    profile: ProfileCard,
    matchBreakdown: MatchBreakdown?,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onOpenPhotos: (List<String>, Int) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val photos = buildList {
        profile.imageURL?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(profile.photos.filter { it.isNotBlank() && it != profile.imageURL })
    }
    val energyColor = when (profile.energyLevel) {
        EnergyLevel.LOW -> EnergyLow
        EnergyLevel.MODERATE -> EnergyModerate
        else -> EnergyFullySocial
    }
    val percentage = matchBreakdown?.percentage ?: profile.matchPercentage
    val firstName = profile.name.substringBefore(' ').ifBlank { profile.name }
    val lookingForItems = profile.lookingFor.filterNot {
        it.equals("Not Sure Yet", ignoreCase = true) || it.equals("Not sure yet", ignoreCase = true)
    }
    var showingMatchDetails by remember { mutableStateOf(false) }
    var showingLookingForDetails by remember { mutableStateOf(false) }

    LaunchedEffect(profile.userId) {
        onRefresh()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(640.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBlock) {
                    Icon(Icons.Filled.Close, contentDescription = "Block", tint = colors.secondaryText)
                }
                Text(
                    text = profile.name,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp,
                )
                IconButton(onClick = onReport) {
                    Icon(Icons.Filled.Report, contentDescription = "Report", tint = colors.secondaryText)
                }
                TextButton(onClick = onDismiss) {
                    Text("Done", color = TabSelected)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box {
                    AsyncImage(
                        model = profile.imageURL ?: photos.firstOrNull(),
                        contentDescription = profile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (photos.isNotEmpty()) onOpenPhotos(photos, 0)
                            },
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (profile.isOnline) EnergyFullySocial
                                    else Color.White.copy(alpha = 0.55f),
                                ),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            stringResource(
                                if (profile.isOnline) {
                                    R.string.discover_online_now
                                } else {
                                    R.string.discover_offline
                                },
                            ),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (photos.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        photos.drop(1).take(4).forEachIndexed { index, url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenPhotos(photos, index + 1) },
                            )
                        }
                    }
                }

                Text(
                    "${profile.name}, ${profile.age}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colors.text,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showingMatchDetails = true },
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = { (percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (percentage > 49) Color(0xFF4CAF50) else DistanceAccent,
                            trackColor = colors.secondaryText.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = buildMatchSentence(
                                percentage = percentage,
                                firstName = firstName,
                                percentColor = if (percentage > 49) Color(0xFF4CAF50) else DistanceAccent,
                            ),
                            color = colors.text,
                            fontSize = 14.sp,
                        )
                    }
                    IconButton(onClick = { showingMatchDetails = true }) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = TabSelected)
                    }
                }
                Text(profile.distance, color = colors.secondaryText)
                Text(
                    "${EnergyLevel.emoji(profile.energyLevel)} ${EnergyLevel.label(profile.energyLevel)}",
                    color = energyColor,
                    fontWeight = FontWeight.Medium,
                )

                SectionCard(title = "About") {
                    Text(profile.bio.ifBlank { "No bio yet" }, color = colors.text, fontSize = 14.sp)
                }

                if (lookingForItems.isNotEmpty()) {
                    SectionCard(title = "Looking for") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = lookingForItems.first(),
                                color = colors.text,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            if (lookingForItems.size > 1) {
                                IconButton(onClick = { showingLookingForDetails = true }) {
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = TabSelected)
                                }
                            }
                        }
                    }
                }

                if (profile.interests.isNotEmpty()) {
                    SectionCard(title = "Interests") {
                        ChipFlow(profile.interests)
                    }
                }

                if (profile.shareLifestyle && profile.lifestyle.isNotBlank() && profile.lifestyle != "preferNot") {
                    SectionCard(title = "Lifestyle") {
                        Text(profile.lifestyle, color = colors.text)
                    }
                }

                if (profile.shareIdealFirstDate && profile.idealFirstDate.isNotEmpty()) {
                    SectionCard(title = "Ideal first date") {
                        ChipFlow(
                            profile.idealFirstDate.map { raw ->
                                IdealFirstDateOption.entries.firstOrNull { it.rawValue == raw }?.displayName
                                    ?: raw
                            },
                        )
                    }
                }

                if (profile.shareSensoryPreferences && profile.sensoryPreferences.isNotEmpty()) {
                    SectionCard(title = "Sensory preferences") {
                        ChipFlow(
                            profile.sensoryPreferences.map {
                                SensoryPreferenceOption.fromRaw(it)?.title ?: it
                            },
                        )
                    }
                }

                matchBreakdown?.let { breakdown ->
                    if (breakdown.commonInterests.isNotEmpty() ||
                        breakdown.commonLookingFor.isNotEmpty() ||
                        breakdown.sharedLifestyle != null
                    ) {
                        SectionCard(title = "Why you match") {
                            if (breakdown.commonInterests.isNotEmpty()) {
                                Text(
                                    "Shared interests: ${breakdown.commonInterests.joinToString()}",
                                    color = colors.text,
                                    fontSize = 13.sp,
                                )
                            }
                            if (breakdown.commonLookingFor.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Shared goals: ${breakdown.commonLookingFor.joinToString()}",
                                    color = colors.text,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = onMessage,
                        modifier = Modifier
                            .weight(1f)
                            .background(TabSelected, RoundedCornerShape(14.dp)),
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Message", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }

    if (showingMatchDetails) {
        MatchDetailsOverlay(
            firstName = firstName,
            breakdown = matchBreakdown,
            onDismiss = { showingMatchDetails = false },
        )
    }
    if (showingLookingForDetails) {
        LookingForDetailsOverlay(
            items = lookingForItems,
            onDismiss = { showingLookingForDetails = false },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = colors.text)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(items: List<String>) {
    val colors = LocalSpectRoomColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Text(
                text = item,
                color = colors.text,
                fontSize = 13.sp,
                modifier = Modifier
                    .background(colors.background, RoundedCornerShape(16.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
