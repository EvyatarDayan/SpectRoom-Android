package com.example.spectroom.ui.screens.discover

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.BodyTypeOption
import com.example.spectroom.data.model.EnergyLevel
import com.example.spectroom.data.model.IdealFirstDateOption
import com.example.spectroom.data.model.LifestyleOption
import com.example.spectroom.data.model.LookingForOption
import com.example.spectroom.data.model.MatchBreakdown
import com.example.spectroom.data.model.PlanTier
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.data.model.ProfileReportReason
import com.example.spectroom.data.model.SensoryPreferenceOption
import com.example.spectroom.ui.screens.profile.ProfileBlueField
import com.example.spectroom.ui.screens.profile.ProfileBlueFieldText
import com.example.spectroom.ui.screens.profile.ProfileEditSectionCard
import com.example.spectroom.ui.screens.profile.ProfileFieldHeading
import com.example.spectroom.ui.screens.profile.ProfileGalleryPhotosCard
import com.example.spectroom.ui.screens.profile.ProfilePhotoStyle
import com.example.spectroom.ui.screens.profile.ProfilePlanPhotoBadge
import com.example.spectroom.ui.screens.profile.ProfileCircleIconButton
import com.example.spectroom.ui.screens.profile.ProfilePillButton
import com.example.spectroom.ui.theme.EnergyFullySocial
import com.example.spectroom.ui.theme.EnergyLow
import com.example.spectroom.ui.theme.EnergyModerate
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.PassRed
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun UserProfileModal(
    profile: ProfileCard,
    matchBreakdown: MatchBreakdown?,
    showsFullMatchBreakdown: Boolean = false,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onOpenPhotos: (List<String>, Int) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onUpgradePlans: () -> Unit = {},
    blockConfirmationFirstName: String? = null,
    onDismissBlockConfirmation: () -> Unit = {},
    onConfirmBlockOnly: () -> Unit = {},
    onConfirmBlockAndReport: () -> Unit = {},
    onCompleteBlockConfirmation: () -> Unit = {},
    showBlockReport: Boolean = false,
    showReportThankYou: Boolean = false,
    onSubmitBlockReport: (ProfileReportReason, String?) -> Unit = { _, _ -> },
    onCancelBlockReport: () -> Unit = {},
    onDismissReportThankYou: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val galleryPhotos = profile.photos.filter { it.isNotBlank() && it != profile.imageURL }
    val allPhotos = buildList {
        profile.imageURL?.takeIf { it.isNotBlank() }?.let(::add)
        addAll(galleryPhotos)
    }
    val percentage = matchBreakdown?.percentage ?: profile.matchPercentage
    val matchColor = if (percentage > 49) LikeGreen else colors.secondaryText
    val firstName = profile.name.substringBefore(' ').ifBlank { profile.name }
    var showingMatchDetails by remember { mutableStateOf(false) }

    LaunchedEffect(profile.userId) {
        onRefresh()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    ProfileCircleIconButton(
                        onClick = onBlock,
                        icon = Icons.Filled.Block,
                        contentDescription = stringResource(R.string.profile_block),
                        iconTint = PassRed,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Text(
                        text = profile.name.ifBlank { stringResource(R.string.profile_my_title) },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 72.dp)
                            .align(Alignment.Center),
                    )
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        ProfilePillButton(
                            text = stringResource(R.string.settings_done),
                            onClick = onDismiss,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProfilePhotoSection(
                        imageUrl = profile.imageURL,
                        planBadge = profile.planTier,
                        onClick = {
                            if (allPhotos.isNotEmpty()) onOpenPhotos(allPhotos, 0)
                        },
                    )

                    ProfileGalleryPhotosCard(
                        photoUrls = galleryPhotos,
                        title = stringResource(R.string.profile_photos),
                        emptyMessage = stringResource(R.string.profile_no_photos),
                        onSelectPhoto = { index ->
                            val start = if (profile.imageURL.isNullOrBlank()) index else index + 1
                            onOpenPhotos(
                                allPhotos,
                                start.coerceAtMost(allPhotos.lastIndex.coerceAtLeast(0)),
                            )
                        },
                    )

                    MatchSectionCard(
                        percentage = percentage,
                        matchColor = matchColor,
                        firstName = firstName,
                        onOpenDetails = { showingMatchDetails = true },
                    )

                    ProfileEditSectionCard {
                        ProfileFieldHeading(stringResource(R.string.profile_age))
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileBlueFieldText(
                            text = "${profile.age} ${stringResource(R.string.profile_years_old)}",
                        )
                    }

                    ProfileEditSectionCard {
                        ProfileFieldHeading(stringResource(R.string.profile_location))
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileBlueFieldText(
                            text = profile.distance.ifBlank {
                                stringResource(R.string.profile_location_not_specified)
                            },
                            secondary = profile.distance.isBlank(),
                        )
                    }

                    EnergySectionCard(energyLevel = profile.energyLevel)

                    PreferencesSectionCard(profile = profile)

                    AboutSectionCard(profile = profile)

                    ProfileEditSectionCard {
                        ProfileFieldHeading(stringResource(R.string.profile_interests))
                        Spacer(modifier = Modifier.height(12.dp))
                        if (profile.interests.isEmpty()) {
                            ProfileBlueFieldText(
                                text = stringResource(R.string.profile_no_interests),
                                secondary = true,
                            )
                        } else {
                            ProfileBlueFieldText(text = profile.interests.joinToString(", "))
                        }
                    }
                }
            }

            if (showingMatchDetails) {
                MatchDetailsOverlay(
                    firstName = firstName,
                    breakdown = matchBreakdown,
                    showsFullBreakdown = showsFullMatchBreakdown,
                    onDismiss = { showingMatchDetails = false },
                    onUpgrade = {
                        showingMatchDetails = false
                        onUpgradePlans()
                    },
                )
            }
            if (blockConfirmationFirstName != null) {
                BlockConfirmationOverlay(
                    firstName = blockConfirmationFirstName,
                    onDismiss = onDismissBlockConfirmation,
                    onBlock = onConfirmBlockOnly,
                    onBlockAndReport = onConfirmBlockAndReport,
                    onBlockComplete = onCompleteBlockConfirmation,
                )
            }
            if (showBlockReport) {
                ReportReasonOverlay(
                    firstName = firstName,
                    onCancel = onCancelBlockReport,
                    onSubmit = onSubmitBlockReport,
                )
            }
            if (showReportThankYou) {
                ReportThankYouOverlay(onDismiss = onDismissReportThankYou)
            }
        }
    }
}

@Composable
private fun ProfilePhotoSection(
    imageUrl: String?,
    planBadge: PlanTier = PlanTier.FREE,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(ProfilePhotoStyle.cardCorner), clip = false)
            .clip(RoundedCornerShape(ProfilePhotoStyle.cardCorner))
            .background(colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProfileFieldHeading(stringResource(R.string.profile_photo))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ProfilePhotoStyle.mainPhotoHeight)
                .clip(RoundedCornerShape(ProfilePhotoStyle.photoCorner))
                .background(Color.Gray.copy(alpha = 0.18f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(56.dp),
                )
            }
            ProfilePlanPhotoBadge(
                tier = planBadge,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun MatchSectionCard(
    percentage: Int,
    matchColor: Color,
    firstName: String,
    onOpenDetails: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    ProfileEditSectionCard {
        ProfileBlueField(
            modifier = Modifier.clickable(onClick = onOpenDetails),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((percentage / 100f).coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(matchColor, RoundedCornerShape(6.dp)),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = buildMatchSentence(
                            percentage = percentage,
                            firstName = firstName,
                            percentColor = matchColor,
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = TabSelected,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergySectionCard(energyLevel: Int) {
    val colors = LocalSpectRoomColors.current
    val energyColor = when (EnergyLevel.resolve(energyLevel)) {
        EnergyLevel.LOW -> EnergyLow
        EnergyLevel.MODERATE -> EnergyModerate
        else -> EnergyFullySocial
    }
    ProfileEditSectionCard {
        ProfileFieldHeading(stringResource(R.string.profile_energy_level))
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(energyColor.copy(alpha = 0.12f))
                .border(1.5.dp, energyColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(EnergyLevel.emoji(energyLevel), fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(EnergyLevel.titleRes(energyLevel)),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = energyColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(EnergyLevel.detailRes(energyLevel)),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = colors.text,
                )
            }
        }
    }
}

@Composable
private fun PreferencesSectionCard(
    profile: ProfileCard,
) {
    val hasGenderOrInterested = profile.gender.isNotBlank() || profile.interestedIn.isNotEmpty()
    val hasLookingFor = profile.lookingFor.isNotEmpty()
    val defaultMin = profile.age
    val defaultMax = minOf(profile.age + 5, 70)
    val hasCustomPreferredAge =
        profile.preferredAgeMin != defaultMin || profile.preferredAgeMax != defaultMax

    ProfileEditSectionCard {
        ProfileFieldHeading(stringResource(R.string.profile_preferences))
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                profile.gender.isNotBlank() && profile.interestedIn.isNotEmpty() -> {
                    ProfileBlueFieldText(
                        text = "${stringResource(R.string.profile_im_a)} ${profile.gender.lowercase()}, " +
                            "${stringResource(R.string.profile_interested_in_label)} ${formatInterestedIn(profile.interestedIn)}",
                        italic = true,
                    )
                }
                profile.gender.isNotBlank() -> {
                    ProfileBlueFieldText(
                        text = "${stringResource(R.string.profile_im_a)} ${profile.gender.lowercase()}",
                        italic = true,
                    )
                }
                profile.interestedIn.isNotEmpty() -> {
                    ProfileBlueFieldText(
                        text = "${stringResource(R.string.profile_interested_in_label).replaceFirstChar { it.uppercase() }} " +
                            formatInterestedIn(profile.interestedIn),
                        italic = true,
                    )
                }
            }

            if (hasLookingFor) {
                ProfileBlueFieldText(
                    text = stringResource(
                        R.string.profile_looking_for_prefix,
                        formatLookingForList(profile.lookingFor),
                    ),
                    italic = true,
                )
            }

            if (hasCustomPreferredAge) {
                ProfileBlueFieldText(
                    text = stringResource(
                        R.string.profile_my_preferred_age,
                        profile.preferredAgeMin.toString(),
                        profile.preferredAgeMax.toString(),
                    ),
                    italic = true,
                )
            }

            if (!hasGenderOrInterested && !hasLookingFor && !hasCustomPreferredAge) {
                ProfileBlueFieldText(
                    text = stringResource(R.string.profile_no_preferences),
                    secondary = true,
                    italic = true,
                )
            }
        }
    }
}

@Composable
private fun AboutSectionCard(profile: ProfileCard) {
    ProfileEditSectionCard {
        ProfileFieldHeading(stringResource(R.string.profile_about_me))
        Spacer(modifier = Modifier.height(12.dp))
        val bio = profile.bio.trim()
        ProfileBlueFieldText(
            text = bio.ifBlank { stringResource(R.string.profile_no_bio) },
            secondary = bio.isBlank(),
        )

        if (profile.shareLifestyle && profile.lifestyle.isNotBlank() && profile.lifestyle != "preferNot") {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileFieldHeading(stringResource(R.string.profile_lifestyle_title))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileBlueFieldText(text = formatLifestyle(profile.lifestyle))
        }

        if (profile.shareBodyType && profile.bodyType.isNotBlank() && profile.bodyType != "preferNot") {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileFieldHeading(stringResource(R.string.profile_body_type))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileBlueFieldText(text = formatBodyType(profile.bodyType))
        }

        if (profile.idealFirstDate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileFieldHeading(stringResource(R.string.profile_ideal_first_date))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileBlueFieldText(text = formatIdealFirstDate(profile.idealFirstDate))
        }

        if (profile.sensoryPreferences.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileFieldHeading(stringResource(R.string.profile_sensory_preferences))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileBlueFieldText(
                text = SensoryPreferenceOption.formattedTitles(
                    profile.sensoryPreferences,
                    LocalContext.current,
                ),
            )
        }
    }
}

@Composable
private fun formatInterestedIn(preferences: List<String>): String {
    if (preferences.any { it.equals("Anyone", ignoreCase = true) }) {
        return stringResource(R.string.profile_interested_anyone)
    }
    if (preferences.isEmpty()) {
        return stringResource(R.string.profile_no_one)
    }
    return preferences.joinToString(" / ") { it.lowercase() }
}

@Composable
private fun formatLookingForList(lookingFor: List<String>): String {
    if (lookingFor.isEmpty()) {
        return stringResource(R.string.profile_nothing_specific)
    }
    return lookingFor.joinToString(" / ") { formatLookingForItem(it) }
}

private fun formatLookingForItem(raw: String): String =
    LookingForOption.entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) }?.displayName
        ?: raw

private fun formatLifestyle(lifestyle: String): String =
    LifestyleOption.entries.firstOrNull { it.rawValue == lifestyle }?.displayName ?: lifestyle

private fun formatBodyType(bodyType: String): String =
    BodyTypeOption.entries.firstOrNull { it.rawValue == bodyType }?.displayName ?: bodyType

private fun formatIdealFirstDate(values: List<String>): String =
    values.joinToString(", ") { raw ->
        IdealFirstDateOption.entries.firstOrNull { it.rawValue == raw }?.displayName ?: raw
    }
