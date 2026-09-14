package com.example.spectroom.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.spectroom.R
import com.example.spectroom.data.model.IdealFirstDateOption
import com.example.spectroom.data.model.LifestyleOption
import com.example.spectroom.data.model.BodyTypeOption
import com.example.spectroom.data.model.LookingForOption
import com.example.spectroom.data.model.PlanTier
import com.example.spectroom.data.model.SensoryPreferenceOption
import com.example.spectroom.data.model.SpectRoomUser
import com.example.spectroom.ui.theme.LocalSpectRoomColors

@Composable
fun ProfileBrowseScreen(
    user: SpectRoomUser?,
    onEdit: () -> Unit,
    onOpenPhotos: (List<String>, Int) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    LaunchedEffect(user?.firebaseDocumentId) {
        onRefresh()
    }

    val photos = buildList {
        user?.profilePhotoURL?.takeIf { it.isNotBlank() }?.let(::add)
        val profileUrl = user?.profilePhotoURL
        user?.photos.orEmpty().forEach { url ->
            if (url.isNotBlank() && url != profileUrl) add(url)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp),
    ) {
        ProfileScreenTopBar(
            title = stringResource(R.string.profile_my_title),
            applyStatusBarPadding = true,
            trailing = {
                ProfileTextLink(
                    text = stringResource(R.string.profile_edit),
                    onClick = onEdit,
                )
            },
        )

        ProfileMainPhotoCard(
            imageUrl = user?.profilePhotoURL,
            planBadge = user?.planTier ?: PlanTier.FREE,
            onClick = if (photos.isNotEmpty()) {
                { onOpenPhotos(photos, 0) }
            } else {
                null
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
        ProfileGalleryPhotosCard(
            photoUrls = user?.photos.orEmpty(),
            title = stringResource(R.string.profile_my_photos),
            emptyMessage = stringResource(R.string.profile_no_photos),
            onSelectPhoto = { index ->
                val start = if (user?.profilePhotoURL.isNullOrBlank()) index else index + 1
                onOpenPhotos(photos, start.coerceAtMost(photos.lastIndex.coerceAtLeast(0)))
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
        ProfileSectionCard(title = stringResource(R.string.profile_my_age)) {
            ProfileBodyText(
                text = "${user?.age ?: 0} ${stringResource(R.string.profile_years_old)}",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileSectionCard(title = stringResource(R.string.profile_my_location)) {
            val location = user?.location?.displayString.orEmpty()
            ProfileBodyText(
                text = location.ifBlank { stringResource(R.string.profile_location_not_specified) },
                secondary = location.isBlank(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileBodyText(
                text = stringResource(R.string.profile_location_privacy_notice),
                secondary = true,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileSectionCard(title = stringResource(R.string.profile_my_preferences)) {
            ProfilePreferencesContent(user)
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileSectionCard(title = stringResource(R.string.profile_about_me)) {
            ProfileAboutMeContent(user)
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileSectionCard(title = stringResource(R.string.profile_my_interests)) {
            if (user?.interests.isNullOrEmpty()) {
                ProfileBodyText(
                    text = stringResource(R.string.profile_no_interests),
                    secondary = true,
                    italic = true,
                )
            } else {
                ProfileBodyText(text = user.interests.joinToString(", "))
            }
        }
    }
}

@Composable
private fun ProfilePreferencesContent(user: SpectRoomUser?) {
    if (user == null) {
        ProfileBodyText(
            text = stringResource(R.string.profile_no_preferences),
            secondary = true,
            italic = true,
        )
        return
    }

    val gender = user.gender.trim()
    val interestedIn = user.interestedIn
    val lookingFor = user.lookingFor
    val hasGenderOrInterested = gender.isNotEmpty() || interestedIn.isNotEmpty()
    val hasLookingFor = lookingFor.isNotEmpty()
    val defaultMin = user.age
    val defaultMax = minOf(user.age + 5, 70)
    val hasCustomPreferredAge =
        user.preferredAgeMin != defaultMin || user.preferredAgeMax != defaultMax

    when {
        gender.isNotEmpty() && interestedIn.isNotEmpty() -> {
            ProfileBodyText(
                text = "${stringResource(R.string.profile_im_a)} ${gender.lowercase()}, " +
                    "${stringResource(R.string.profile_interested_in_label)} ${formatInterestedIn(interestedIn)}",
                italic = true,
            )
        }
        gender.isNotEmpty() -> {
            ProfileBodyText(
                text = "${stringResource(R.string.profile_im_a)} ${gender.lowercase()}",
                italic = true,
            )
        }
        interestedIn.isNotEmpty() -> {
            ProfileBodyText(
                text = "${stringResource(R.string.profile_interested_in_label).replaceFirstChar { it.uppercase() }} " +
                    formatInterestedIn(interestedIn),
                italic = true,
            )
        }
    }

    if (hasGenderOrInterested && hasLookingFor) {
        ProfilePreferenceDivider()
    }
    if (hasLookingFor) {
        ProfileBodyText(
            text = "${stringResource(R.string.profile_looking_for_label)} ${formatLookingFor(lookingFor)}",
            italic = true,
        )
    }

    if (hasLookingFor && hasCustomPreferredAge) {
        ProfilePreferenceDivider()
    }
    if (hasCustomPreferredAge) {
        ProfileBodyText(
            text = stringResource(
                R.string.profile_my_preferred_age,
                user.preferredAgeMin.toString(),
                user.preferredAgeMax.toString(),
            ),
            italic = true,
        )
    }

    if (!hasGenderOrInterested && !hasLookingFor && !hasCustomPreferredAge) {
        ProfileBodyText(
            text = stringResource(R.string.profile_no_preferences),
            secondary = true,
            italic = true,
        )
    }
}

@Composable
private fun ProfileAboutMeContent(user: SpectRoomUser?) {
    ProfileBodyText(
        text = user?.bio?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.profile_no_bio),
        secondary = user?.bio.isNullOrBlank(),
        italic = user?.bio.isNullOrBlank() == true,
    )

    if (user?.shareLifestyle == true && !user.lifestyle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        ProfileSubsectionTitle(stringResource(R.string.profile_my_lifestyle_title))
        Spacer(modifier = Modifier.height(8.dp))
        ProfileBodyText(text = formatLifestyle(user.lifestyle))
    }

    if (user?.shareBodyType == true && !user.bodyType.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        ProfileSubsectionTitle(stringResource(R.string.profile_my_body_type))
        Spacer(modifier = Modifier.height(8.dp))
        ProfileBodyText(text = formatBodyType(user.bodyType))
    }

    if (!user?.idealFirstDate.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ProfileSubsectionTitle(stringResource(R.string.profile_my_ideal_first_date))
        Spacer(modifier = Modifier.height(8.dp))
        ProfileBodyText(text = formatIdealFirstDate(user.idealFirstDate))
    }

    if (!user?.sensoryPreferences.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ProfileSubsectionTitle(stringResource(R.string.profile_my_sensory_preferences))
        Spacer(modifier = Modifier.height(8.dp))
        ProfileBodyText(
            text = SensoryPreferenceOption.formattedTitles(
                user.sensoryPreferences,
                LocalContext.current,
            ),
        )
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
private fun formatLookingFor(lookingFor: List<String>): String {
    if (lookingFor.isEmpty()) {
        return stringResource(R.string.profile_nothing_specific)
    }
    return lookingFor.joinToString(" / ") { raw ->
        LookingForOption.entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) }?.displayName
            ?: raw
    }
}

private fun formatLifestyle(lifestyle: String): String =
    LifestyleOption.entries.firstOrNull { it.rawValue == lifestyle }?.displayName ?: lifestyle

private fun formatBodyType(bodyType: String): String =
    BodyTypeOption.entries.firstOrNull { it.rawValue == bodyType }?.displayName ?: bodyType

private fun formatIdealFirstDate(values: List<String>): String =
    values.joinToString(", ") { raw ->
        IdealFirstDateOption.entries.firstOrNull { it.rawValue == raw }?.displayName ?: raw
    }
