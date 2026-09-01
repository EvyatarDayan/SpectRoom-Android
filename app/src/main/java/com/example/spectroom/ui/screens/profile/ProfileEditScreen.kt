package com.example.spectroom.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.spectroom.R
import com.example.spectroom.data.model.GenderPreferenceOption
import com.example.spectroom.data.model.BodyTypeOption
import com.example.spectroom.data.model.EnergyLevel
import com.example.spectroom.data.model.IdealFirstDateOption
import com.example.spectroom.data.model.LifestyleOption
import com.example.spectroom.data.model.LookingForOption
import com.example.spectroom.data.model.SensoryPreferenceOption
import com.example.spectroom.data.model.SpectRoomUser
import com.example.spectroom.domain.ProfileCompletion
import com.example.spectroom.domain.ProfileValidation
import com.example.spectroom.domain.ProfileValidationSection
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.PassRed
import com.example.spectroom.ui.theme.TabSelected
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.delay

private val suggestedInterests = listOf(
    "Photography", "Music", "Art", "Reading", "Cooking", "Travel", "Sports", "Gaming",
    "Movies", "Nature", "Technology", "Fitness", "Dancing", "Writing", "Gardening",
    "Animals", "Science", "History", "Languages", "Volunteering",
)

private val genderOptions = GenderPreferenceOption.all

private fun genderLabelRes(value: String): Int = when (value) {
    "Man" -> R.string.profile_edit_gender_man
    "Woman" -> R.string.profile_edit_gender_woman
    "Non-binary" -> R.string.profile_edit_gender_nonbinary
    else -> R.string.profile_edit_gender_man
}

@Composable
private fun bioCharacterCounter(bio: String): String {
    val count = bio.length
    return when {
        count < 30 -> stringResource(R.string.profile_edit_character_counter_min, (30 - count).toString())
        count >= 500 -> stringResource(R.string.profile_edit_character_counter_max, "500")
        else -> stringResource(R.string.profile_edit_character_counter_max, (500 - count).toString())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileEditScreen(
    user: SpectRoomUser?,
    mandatory: Boolean = false,
    isUploadingPhoto: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    showWelcomeOverlay: Boolean = false,
    onDismissWelcome: () -> Unit = {},
    onSave: (SpectRoomUser) -> Unit = {},
    onCancel: (() -> Unit)? = null,
    onRequestProfilePhotoPick: () -> Unit = {},
    onRequestGalleryPhotoPick: () -> Unit = {},
    onDeleteGalleryPhoto: (String) -> Unit = {},
    onSearchAddress: () -> Unit = {},
    onRequestDateOfBirthPick: (Long, (Long) -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
) {
    val colors = LocalSpectRoomColors.current
    var name by remember(user?.firebaseDocumentId, user?.name) { mutableStateOf(user?.name.orEmpty()) }
    var dateOfBirth by remember(user?.firebaseDocumentId, user?.dateOfBirth, user?.age) {
        mutableStateOf(initialDateOfBirth(user))
    }
    var isDobExpanded by remember { mutableStateOf(false) }
    val age = remember(dateOfBirth) { calculateAge(dateOfBirth) }
    var bio by remember(user?.firebaseDocumentId, user?.bio) { mutableStateOf(user?.bio.orEmpty()) }
    var gender by remember(user?.firebaseDocumentId, user?.gender) { mutableStateOf(user?.gender.orEmpty()) }
    var interests by remember(user?.firebaseDocumentId, user?.interests) {
        mutableStateOf(user?.interests.orEmpty())
    }
    var interestedIn by remember(user?.firebaseDocumentId, user?.interestedIn) {
        mutableStateOf(GenderPreferenceOption.normalizeInterestedIn(user?.interestedIn.orEmpty()))
    }
    var lookingFor by remember(user?.firebaseDocumentId, user?.lookingFor) {
        mutableStateOf(user?.lookingFor.orEmpty())
    }
    var idealFirstDate by remember(user?.firebaseDocumentId, user?.idealFirstDate) {
        mutableStateOf(user?.idealFirstDate.orEmpty())
    }
    var energyLevel by remember(user?.firebaseDocumentId, user?.energyLevel) {
        mutableIntStateOf(user?.energyLevel ?: EnergyLevel.FULLY_SOCIAL)
    }
    var preferredAgeMin by remember(user?.firebaseDocumentId, user?.preferredAgeMin, user?.preferredAgeMax, user?.age) {
        val (min, _) = PreferredAgeRangeLogic.initialRange(
            userAge = user?.age?.takeIf { it >= 18 } ?: 18,
            savedMin = user?.preferredAgeMin,
            savedMax = user?.preferredAgeMax,
        )
        mutableIntStateOf(min)
    }
    var preferredAgeMax by remember(user?.firebaseDocumentId, user?.preferredAgeMin, user?.preferredAgeMax, user?.age) {
        val (_, max) = PreferredAgeRangeLogic.initialRange(
            userAge = user?.age?.takeIf { it >= 18 } ?: 18,
            savedMin = user?.preferredAgeMin,
            savedMax = user?.preferredAgeMax,
        )
        mutableIntStateOf(max)
    }
    var lifestyle by remember(user?.firebaseDocumentId, user?.lifestyle) {
        mutableStateOf(user?.lifestyle.orEmpty())
    }
    var shareLifestyle by remember(user?.firebaseDocumentId, user?.shareLifestyle) {
        mutableStateOf(user?.shareLifestyle == true)
    }
    var bodyType by remember(user?.firebaseDocumentId, user?.bodyType) {
        mutableStateOf(user?.bodyType.orEmpty())
    }
    var shareBodyType by remember(user?.firebaseDocumentId, user?.shareBodyType) {
        mutableStateOf(user?.shareBodyType == true)
    }
    var sensoryPreferences by remember(user?.firebaseDocumentId, user?.sensoryPreferences) {
        mutableStateOf(user?.sensoryPreferences.orEmpty())
    }
    var shareSensory by remember(user?.firebaseDocumentId, user?.shareSensoryPreferences) {
        mutableStateOf(user?.shareSensoryPreferences == true)
    }
    var shareIdealFirstDate by remember(user?.firebaseDocumentId, user?.shareIdealFirstDate) {
        mutableStateOf(user?.shareIdealFirstDate == true)
    }

    val completionPercent = ProfileCompletion.percentage(
        name = name,
        age = age,
        locationDisplay = user?.location?.displayString.orEmpty(),
        locationLat = user?.location?.latitude,
        locationLon = user?.location?.longitude,
        bio = bio,
        profilePhotoURL = user?.profilePhotoURL,
        photos = user?.photos.orEmpty(),
        interests = interests,
        gender = gender,
        interestedIn = interestedIn,
        lookingFor = lookingFor,
        idealFirstDate = idealFirstDate,
    )
    val isFormValid = completionPercent == 100

    var hasAttemptedSave by remember { mutableStateOf(false) }
    var scrollToValidationSection by remember { mutableStateOf<ProfileValidationSection?>(null) }
    val bringIntoViewRequesters = remember {
        ProfileValidationSection.entries.associateWith { BringIntoViewRequester() }
    }

    val resolvedName = name.trim().ifBlank { user?.name?.trim().orEmpty() }
    val validation = ProfileValidation.evaluate(
        name = resolvedName,
        age = age,
        locationDisplay = user?.location?.displayString.orEmpty(),
        locationLat = user?.location?.latitude,
        locationLon = user?.location?.longitude,
        bio = bio,
        profilePhotoURL = user?.profilePhotoURL,
        photos = user?.photos.orEmpty(),
        interests = interests,
        gender = gender,
        interestedIn = interestedIn,
        lookingFor = lookingFor,
        idealFirstDate = idealFirstDate,
    )

    LaunchedEffect(scrollToValidationSection) {
        val section = scrollToValidationSection ?: return@LaunchedEffect
        delay(100)
        bringIntoViewRequesters[section]?.bringIntoView()
        scrollToValidationSection = null
    }

    fun sectionModifier(
        section: ProfileValidationSection,
        isValid: Boolean,
        cornerRadius: androidx.compose.ui.unit.Dp = ProfileFieldStyle.sectionCorner,
    ): Modifier = Modifier
        .fillMaxWidth()
        .bringIntoViewRequester(bringIntoViewRequesters.getValue(section))
        .profileValidationBorder(
            isValid = isValid,
            hasAttemptedSave = hasAttemptedSave,
            cornerRadius = cornerRadius,
        )

    var isCompletionBarVisible by remember { mutableStateOf(false) }
    var hasResolvedCompletionBarVisibility by remember { mutableStateOf(false) }
    var suppressCompletionBarUntilIncompleteSave by remember { mutableStateOf(false) }
    var previousCompletionPercent by remember { mutableIntStateOf(-1) }
    var wasSaving by remember { mutableStateOf(false) }

    val shouldShowCompletionBar = hasResolvedCompletionBarVisibility && isCompletionBarVisible

    LaunchedEffect(user?.firebaseDocumentId) {
        val currentUser = user
        if (currentUser != null) {
            val loadedPercent = ProfileCompletion.percentage(currentUser)
            if (loadedPercent == 100) {
                suppressCompletionBarUntilIncompleteSave = true
                isCompletionBarVisible = false
            } else {
                suppressCompletionBarUntilIncompleteSave = false
                isCompletionBarVisible = true
            }
            hasResolvedCompletionBarVisibility = true
            previousCompletionPercent = loadedPercent
        } else {
            hasResolvedCompletionBarVisibility = false
            previousCompletionPercent = -1
        }
    }

    LaunchedEffect(completionPercent, hasResolvedCompletionBarVisibility, suppressCompletionBarUntilIncompleteSave) {
        if (!hasResolvedCompletionBarVisibility) return@LaunchedEffect
        if (previousCompletionPercent != -1 &&
            previousCompletionPercent != completionPercent &&
            !suppressCompletionBarUntilIncompleteSave
        ) {
            isCompletionBarVisible = true
        }
        previousCompletionPercent = completionPercent
    }

    LaunchedEffect(isSaving, saveError) {
        if (wasSaving && !isSaving && saveError == null) {
            suppressCompletionBarUntilIncompleteSave = true
            isCompletionBarVisible = false
        }
        wasSaving = isSaving
    }

    val performSave: () -> Unit = {
        if (!isFormValid) {
            hasAttemptedSave = true
            suppressCompletionBarUntilIncompleteSave = false
            isCompletionBarVisible = true
            hasResolvedCompletionBarVisibility = true
            scrollToValidationSection = validation.firstMissingSection()
        } else {
            val base = user ?: SpectRoomUser(email = "")
            val (clampedMin, clampedMax) = PreferredAgeRangeLogic.clampRange(preferredAgeMin, preferredAgeMax)
            onSave(
                base.copy(
                name = name.trim(),
                age = age,
                dateOfBirth = dateOfBirth,
                bio = bio.trim(),
                gender = gender,
                interests = interests,
                interestedIn = GenderPreferenceOption.normalizeInterestedIn(interestedIn),
                lookingFor = lookingFor,
                idealFirstDate = idealFirstDate,
                shareIdealFirstDate = shareIdealFirstDate,
                lifestyle = lifestyle,
                shareLifestyle = shareLifestyle,
                bodyType = bodyType,
                shareBodyType = shareBodyType,
                sensoryPreferences = sensoryPreferences,
                shareSensoryPreferences = shareSensory,
                energyLevel = energyLevel,
                preferredAgeMin = clampedMin,
                preferredAgeMax = clampedMax,
                ),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            ProfileScreenTopBar(
                title = if (mandatory) {
                    stringResource(R.string.profile_complete_welcome_title)
                } else {
                    stringResource(R.string.profile_edit_title)
                },
                applyStatusBarPadding = true,
                leading = if (onCancel != null) {
                    {
                        ProfilePillButton(
                            text = stringResource(R.string.common_cancel),
                            onClick = onCancel,
                            enabled = !isSaving,
                        )
                    }
                } else {
                    null
                },
                trailing = {
                    ProfilePillButton(
                        text = stringResource(R.string.profile_edit_save),
                        onClick = { performSave() },
                        emphasized = true,
                        enabled = !isSaving,
                    )
                },
            )

            AnimatedVisibility(
                visible = shouldShowCompletionBar,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
            ) {
                Column {
                    ProfileCompletionBar(percent = completionPercent)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (mandatory) 24.dp else 110.dp),
            ) {
        Column(
            modifier = sectionModifier(
                section = ProfileValidationSection.PROFILE_PHOTO,
                isValid = validation.isProfilePhotoValid,
                cornerRadius = ProfilePhotoStyle.cardCorner,
            )
                .shadow(8.dp, RoundedCornerShape(ProfilePhotoStyle.cardCorner), clip = false)
                .clip(RoundedCornerShape(ProfilePhotoStyle.cardCorner))
                .background(colors.card)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_edit_profile_photo),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center) {
                ProfileMainPhotoCard(
                    imageUrl = user?.profilePhotoURL,
                    showsCameraBadge = true,
                    placeholderText = stringResource(R.string.profile_edit_add_photo),
                    onClick = onRequestProfilePhotoPick,
                )
                if (isUploadingPhoto) {
                    CircularProgressIndicator(color = TabSelected, modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = sectionModifier(
                section = ProfileValidationSection.PHOTOS,
                isValid = validation.isPhotosValid,
                cornerRadius = ProfilePhotoStyle.cardCorner,
            ),
        ) {
            ProfileGalleryPhotosCard(
                photoUrls = user?.photos.orEmpty(),
                title = stringResource(R.string.profile_my_photos),
                footerText = stringResource(R.string.profile_edit_photos_hint),
                onAddPhoto = onRequestGalleryPhotoPick,
                onDeletePhoto = onDeleteGalleryPhoto,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val displayName = resolvedName.ifBlank { "—" }
        val displayEmail = user?.email?.trim().orEmpty().ifBlank { "—" }
        val locationText = user?.location?.displayString?.trim().orEmpty().ifBlank {
            stringResource(R.string.profile_edit_location_placeholder)
        }
        val genderItems = genderOptions.map { value ->
            value to stringResource(genderLabelRes(value))
        }

        ProfileEditSectionCard(
            modifier = sectionModifier(
                section = ProfileValidationSection.NAME,
                isValid = validation.isNameValid,
            ),
        ) {
            ProfileFieldHeading(stringResource(R.string.profile_edit_name))
            Spacer(modifier = Modifier.height(14.dp))
            ProfileReadOnlyField(text = displayName)
            Spacer(modifier = Modifier.height(18.dp))
            ProfileFieldHeading(stringResource(R.string.profile_edit_email))
            Spacer(modifier = Modifier.height(14.dp))
            ProfileReadOnlyField(text = displayEmail)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileFieldCaption(stringResource(R.string.profile_email_privacy_notice))
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard(
            modifier = sectionModifier(
                section = ProfileValidationSection.AGE,
                isValid = validation.isAgeValid,
            ),
        ) {
            ProfileFieldHeading(stringResource(R.string.profile_edit_date_of_birth))
            Spacer(modifier = Modifier.height(14.dp))
            ProfileDateOfBirthField(
                dateText = DateFormat.getDateInstance(DateFormat.LONG).format(dateOfBirth),
                ageText = stringResource(R.string.profile_edit_my_age, age.toString()),
                ageIsValid = age >= 18,
                expanded = isDobExpanded,
                onClick = {
                    isDobExpanded = true
                    onRequestDateOfBirthPick(dateOfBirth.time, { pickedMillis ->
                        dateOfBirth = Date(pickedMillis)
                    }, {
                        isDobExpanded = false
                    })
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard(
            modifier = sectionModifier(
                section = ProfileValidationSection.LOCATION,
                isValid = validation.isLocationValid,
            ),
        ) {
            ProfileFieldHeading(stringResource(R.string.profile_my_location))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileLocationField(
                locationText = locationText,
                onClick = onSearchAddress,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProfileFieldCaption(stringResource(R.string.profile_location_privacy_notice))
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard(
            modifier = sectionModifier(
                section = ProfileValidationSection.GENDER_PREFERENCES,
                isValid = validation.isGenderPreferencesValid,
            ),
        ) {
            ProfileFieldHeading(stringResource(R.string.profile_my_preferences))
            Spacer(modifier = Modifier.height(16.dp))
            ProfileFieldSubheading(stringResource(R.string.profile_edit_i_am))
            Spacer(modifier = Modifier.height(8.dp))
            ProfileChipGrid(
                items = genderItems,
                selected = listOfNotNull(gender.takeIf { it.isNotBlank() }),
                onToggle = { gender = it },
                columns = 2,
            )

            ProfileSectionDivider()
            ProfileFieldSubheading(stringResource(R.string.profile_edit_interested_in))
            Spacer(modifier = Modifier.height(8.dp))
            ProfileChipGrid(
                items = genderItems,
                selected = interestedIn,
                onToggle = { interestedIn = interestedIn.toggle(it) },
                columns = 2,
            )
            if (hasAttemptedSave && !validation.isInterestedInValid) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.profile_edit_validation_interested_in),
                    color = PassRed,
                    fontSize = 12.sp,
                )
            }

            ProfileSectionDivider()
            ProfileFieldSubheading(stringResource(R.string.profile_edit_looking_for))
            Spacer(modifier = Modifier.height(8.dp))
            ProfileChipList(
                items = LookingForOption.entries.map { it.rawValue to it.displayName },
                selected = lookingFor,
                onToggle = { lookingFor = lookingFor.toggle(it) },
            )

            ProfileSectionDivider()
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileFieldSubheading(stringResource(R.string.profile_edit_preferred_age))
                ProfilePreferredAgeBadge(rangeText = "$preferredAgeMin–$preferredAgeMax")
                PreferredAgeRangeSlider(
                    minAge = preferredAgeMin,
                    maxAge = preferredAgeMax,
                    onRangeChange = { min, max ->
                        val (clampedMin, clampedMax) = PreferredAgeRangeLogic.clampRange(min, max)
                        preferredAgeMin = clampedMin
                        preferredAgeMax = clampedMax
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard(
            modifier = sectionModifier(
                section = ProfileValidationSection.ABOUT_ME,
                isValid = validation.isBioValid,
            ),
        ) {
            ProfileFieldHeading(stringResource(R.string.profile_edit_about_me))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileBioField(
                value = bio,
                onValueChange = { bio = it },
                characterCounter = bioCharacterCounter(bio),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard {
            ProfileFieldHeading(stringResource(R.string.profile_my_lifestyle_title))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileChipListSingle(
                items = LifestyleOption.entries.map { it.rawValue to it.displayName },
                selected = lifestyle.takeIf { it.isNotBlank() },
                onSelect = { lifestyle = it },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard {
            ProfileFieldHeading(stringResource(R.string.profile_my_body_type))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileChipListSingle(
                items = BodyTypeOption.entries.map { it.rawValue to it.displayName },
                selected = bodyType.takeIf { it.isNotBlank() },
                onSelect = { bodyType = it },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard(
            modifier = sectionModifier(
                section = ProfileValidationSection.IDEAL_FIRST_DATE,
                isValid = validation.isIdealFirstDateValid,
            ),
        ) {
            ProfileFieldHeading(stringResource(R.string.profile_my_ideal_first_date))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileChipList(
                items = IdealFirstDateOption.entries.map { it.rawValue to it.displayName },
                selected = idealFirstDate,
                onToggle = { idealFirstDate = idealFirstDate.toggle(it) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard {
            ProfileFieldHeading(stringResource(R.string.profile_my_sensory_preferences))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileChipList(
                items = SensoryPreferenceOption.entries.map { it.rawValue to it.title },
                selected = sensoryPreferences,
                onToggle = { sensoryPreferences = sensoryPreferences.toggle(it) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProfileEditSectionCard(
            modifier = sectionModifier(
                section = ProfileValidationSection.INTERESTS,
                isValid = validation.isInterestsValid,
            ),
        ) {
            ProfileFieldHeading(stringResource(R.string.profile_my_interests))
            Spacer(modifier = Modifier.height(12.dp))
            ProfileChipGrid(
                items = suggestedInterests.map { it to it },
                selected = interests,
                onToggle = { interests = interests.toggle(it) },
                columns = 2,
            )
        }
        }
    }

    if (isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.card.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = TabSelected,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.profile_edit_saving),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                )
            }
        }
    }

    if (showWelcomeOverlay) {
        CompleteProfileWelcomeOverlay(onContinue = onDismissWelcome)
    }
    }
}


private fun List<String>.toggle(value: String): List<String> =
    if (any { it.equals(value, ignoreCase = true) }) {
        filterNot { it.equals(value, ignoreCase = true) }
    } else {
        this + value
    }

private fun initialDateOfBirth(user: SpectRoomUser?): Date {
    user?.dateOfBirth?.let { return it }
    val age = user?.age?.takeIf { it >= 18 } ?: 25
    return Calendar.getInstance().apply {
        add(Calendar.YEAR, -age)
    }.time
}

private fun calculateAge(dob: Date): Int {
    val birth = Calendar.getInstance().apply { time = dob }
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
        age -= 1
    }
    return age.coerceAtLeast(0)
}
