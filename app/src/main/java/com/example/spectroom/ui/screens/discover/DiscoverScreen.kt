package com.example.spectroom.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.local.DiscoverDailyLimitStore
import com.example.spectroom.data.model.DiscoverDistanceOption
import com.example.spectroom.data.model.EnergyLevel
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.data.model.SwipeType
import com.example.spectroom.ui.components.TabBarBodyHeight
import com.example.spectroom.ui.theme.DistanceAccent
import com.example.spectroom.ui.theme.EnergyFullySocial
import com.example.spectroom.ui.theme.EnergyLow
import com.example.spectroom.ui.theme.EnergyModerate
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.PassRed
import com.example.spectroom.viewmodel.DiscoverUiState
import kotlin.math.roundToInt

private val CardCorner = 24.dp
private val PhotoHeight = 340.dp
private val CardHeight = 500.dp
private val CardHorizontalInset = 30.dp
private val CardAreaTopPadding = 150.dp
private val ActionRowBottomPadding = TabBarBodyHeight
private val CardAreaBottomPadding = 108.dp
private val SwipeStampYOffset = 52.dp

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    currentEnergyLevel: Int = EnergyLevel.FULLY_SOCIAL,
    profileIncomplete: Boolean = false,
    onSwipe: (SwipeType) -> Unit,
    onUndo: () -> Unit,
    onSecondThoughts: () -> Unit,
    onOpenProfile: (ProfileCard) -> Unit = {},
    onUpdateEnergyLevel: (Int) -> Unit = {},
    onSetDistance: (DiscoverDistanceOption) -> Unit = {},
    onFinishProfile: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val profile = state.profiles.getOrNull(state.currentIndex)
    val reachedLimit = state.dailySwipeCount >= DiscoverDailyLimitStore.MAX_SWIPES_PER_DAY &&
        !state.isReviewingTodaysPasses
    var showingEnergyPicker by remember { mutableStateOf(false) }
    var showingDistancePicker by remember { mutableStateOf(false) }
    val showActions = !profileIncomplete && !reachedLimit && profile != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Card stack — iOS pulls the deck up with negative top inset; mirror with offset + insets.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = CardHorizontalInset,
                    end = CardHorizontalInset,
                    top = CardAreaTopPadding,
                    bottom = CardAreaBottomPadding,
                )
                .offset(y = (-36).dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (state.isReviewingTodaysPasses) {
                Text(
                    text = stringResource(R.string.discover_second_thoughts_banner),
                    color = DistanceAccent,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(bottom = 8.dp),
                )
            }

            when {
                state.isLoading -> androidx.compose.material3.CircularProgressIndicator(
                    color = DistanceAccent,
                    modifier = Modifier.align(Alignment.Center),
                )
                profileIncomplete -> IncompleteProfileCard(onFinishProfile = onFinishProfile)
                reachedLimit -> DailyLimitCard(
                    canUseSecondThoughts = !state.hasUsedSecondThoughtsToday,
                    isLoadingSecondThoughts = state.isLoadingSecondThoughts,
                    errorMessage = state.secondThoughtsError,
                    onSecondThoughts = onSecondThoughts,
                )
                profile == null -> EmptyDeckCard(modifier = Modifier.align(Alignment.Center))
                else -> {
                    // iOS pile: front card + up to 2 behind (scale down + peek upward)
                    val start = state.currentIndex
                    val end = minOf(start + 3, state.profiles.size)
                    // Draw back → front so z-order is correct without relying only on zIndex
                    for (index in (end - 1) downTo start) {
                        val depth = index - start
                        val stacked = state.profiles[index]
                        SwipeableProfileCard(
                            profile = stacked,
                            depth = depth,
                            isTopCard = depth == 0,
                            onSwiped = onSwipe,
                            onOpen = { onOpenProfile(stacked) },
                        )
                    }
                }
            }
        }

        if (showActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ActionRowBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionCircle(
                    icon = Icons.Filled.Close,
                    tint = PassRed,
                    onClick = { onSwipe(SwipeType.PASS) },
                    size = 76.dp,
                    iconSize = 30.dp,
                )
                ActionCircle(
                    icon = Icons.Filled.Replay,
                    tint = colors.text,
                    enabled = state.canUndo,
                    onClick = onUndo,
                    size = 52.dp,
                    iconSize = 18.dp,
                )
                ActionCircle(
                    icon = Icons.Filled.Favorite,
                    tint = LikeGreen,
                    onClick = { onSwipe(SwipeType.LIKE) },
                    size = 76.dp,
                    iconSize = 30.dp,
                )
            }
        }

        // Corner pills — aligned with card horizontal inset on iOS
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.card)
                .border(1.dp, colors.floatingPillBorder, RoundedCornerShape(22.dp))
                .clickable {
                    showingDistancePicker = !showingDistancePicker
                    if (showingDistancePicker) showingEnergyPicker = false
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "📍", fontSize = 14.sp)
            Text(
                text = state.distanceOption.shortButtonTitle,
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.dp, colors.floatingPillBorder, CircleShape)
                .clickable {
                    showingEnergyPicker = !showingEnergyPicker
                    if (showingEnergyPicker) showingDistancePicker = false
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = EnergyLevel.emoji(currentEnergyLevel), fontSize = 20.sp)
        }

        if (showingEnergyPicker) {
            EnergyLevelPickerOverlay(
                selectedLevel = currentEnergyLevel,
                onSelect = { level ->
                    onUpdateEnergyLevel(level)
                    showingEnergyPicker = false
                },
                onDismiss = { showingEnergyPicker = false },
            )
        }

        if (showingDistancePicker) {
            DistancePickerOverlay(
                selected = state.distanceOption,
                onSelect = { option ->
                    onSetDistance(option)
                    showingDistancePicker = false
                },
                onDismiss = { showingDistancePicker = false },
            )
        }
    }
}

@Composable
private fun DistancePickerOverlay(
    selected: DiscoverDistanceOption,
    onSelect: (DiscoverDistanceOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.card)
                .clickable(enabled = false) {}
                .padding(20.dp),
        ) {
            Text(
                stringResource(R.string.discover_distance_title),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.discover_distance_subtitle),
                color = colors.secondaryText,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DiscoverDistanceOption.entries.forEach { option ->
                val isSelected = option == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) DistanceAccent.copy(alpha = 0.18f) else colors.background,
                        )
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) DistanceAccent else colors.border,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (option) {
                            DiscoverDistanceOption.FIVE_KM -> stringResource(R.string.settings_distance_5km)
                            DiscoverDistanceOption.TEN_KM -> stringResource(R.string.settings_distance_10km)
                            DiscoverDistanceOption.TWENTY_KM -> stringResource(R.string.settings_distance_20km)
                            DiscoverDistanceOption.FIFTY_KM -> stringResource(R.string.settings_distance_50km)
                            DiscoverDistanceOption.UNLIMITED -> stringResource(R.string.settings_distance_unlimited)
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        option.shortButtonTitle,
                        color = if (isSelected) DistanceAccent else colors.secondaryText,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyLevelPickerOverlay(
    selectedLevel: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.card)
                .clickable(enabled = false) {}
                .padding(20.dp),
        ) {
            Text(
                stringResource(R.string.discover_energy_title),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.discover_energy_subtitle),
                color = colors.secondaryText,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            EnergyLevel.all.forEach { level ->
                val selected = EnergyLevel.resolve(selectedLevel) == level
                val accent = when (level) {
                    EnergyLevel.LOW -> EnergyLow
                    EnergyLevel.MODERATE -> EnergyModerate
                    else -> EnergyFullySocial
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) accent.copy(alpha = 0.18f) else colors.background,
                        )
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) accent else colors.border,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelect(level) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(EnergyLevel.emoji(level), fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            EnergyLevel.label(level),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text,
                        )
                        Text(
                            EnergyLevel.detail(level),
                            color = colors.secondaryText,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableProfileCard(
    profile: ProfileCard,
    depth: Int = 0,
    isTopCard: Boolean = true,
    onSwiped: (SwipeType) -> Unit,
    onOpen: () -> Unit,
) {
    var offsetX by remember(profile.userId) { mutableFloatStateOf(0f) }
    var offsetY by remember(profile.userId) { mutableFloatStateOf(0f) }
    val rotation = if (isTopCard) offsetX / 20f else 0f
    val density = LocalDensity.current
    val threshold = with(density) { 100.dp.toPx() }
    var totalDrag by remember(profile.userId) { mutableFloatStateOf(0f) }

    // Match iOS StackAppearance: scale = max(0.95, 1 - depth*0.02), y = depth * -24
    val stackScale = maxOf(0.95f, 1f - depth * 0.02f)
    val stackYOffsetPx = with(density) { (depth * -24).dp.toPx() }
    val stackAlpha = if (depth == 0) 1f else 0.95f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .zIndex((3 - depth).toFloat())
            .graphicsLayer {
                scaleX = stackScale
                scaleY = stackScale
                translationY = stackYOffsetPx
                alpha = stackAlpha
            }
            .offset {
                IntOffset(
                    if (isTopCard) offsetX.roundToInt() else 0,
                    if (isTopCard) offsetY.roundToInt() else 0,
                )
            }
            .rotate(rotation)
            .then(
                if (isTopCard) {
                    Modifier.pointerInput(profile.userId) {
                        detectDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                when {
                                    offsetX > threshold -> onSwiped(SwipeType.LIKE)
                                    offsetX < -threshold -> onSwiped(SwipeType.PASS)
                                    totalDrag < 12f -> {
                                        offsetX = 0f
                                        offsetY = 0f
                                        onOpen()
                                    }
                                    else -> {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y * 0.25f
                                totalDrag += kotlin.math.abs(dragAmount.x) +
                                    kotlin.math.abs(dragAmount.y)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        ProfileCardContent(profile = profile)

        if (isTopCard) {
            val stampAlpha = (kotlin.math.abs(offsetX) / 170f).coerceIn(0f, 1f)
            if (offsetX > 40 || offsetX < -40) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(PhotoHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    if (offsetX > 40) {
                        SwipeStamp(
                            text = "Like",
                            color = LikeGreen.copy(alpha = stampAlpha),
                            modifier = Modifier.offset(y = SwipeStampYOffset),
                        )
                    } else {
                        SwipeStamp(
                            text = "Pass",
                            color = PassRed.copy(alpha = stampAlpha),
                            modifier = Modifier.offset(y = SwipeStampYOffset),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCardContent(profile: ProfileCard) {
    val colors = LocalSpectRoomColors.current
    val firstName = profile.name.substringBefore(' ').ifBlank { profile.name }
    val photoUrl = profile.imageURL ?: profile.photos.firstOrNull()
    val matchColor = if (profile.matchPercentage > 49) LikeGreen else colors.secondaryText
    val matchPercentText = formatSpacedMatchPercent(profile.matchPercentage)
    val matchSentence = buildAnnotatedString {
        append(stringResource(R.string.search_match_prefix))
        withStyle(SpanStyle(color = matchColor, fontWeight = FontWeight.SemiBold)) {
            append(matchPercentText)
        }
        append(stringResource(R.string.search_match_label))
        append(stringResource(R.string.search_match_suffix, firstName))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(CardCorner),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(CardCorner))
            .background(colors.card),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PhotoHeight)
                .clip(RoundedCornerShape(topStart = CardCorner, topEnd = CardCorner)),
        ) {
            if (photoUrl.isNullOrBlank()) {
                PhotoPlaceholder()
            } else {
                SubcomposeAsyncImage(
                    model = photoUrl,
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { PhotoPlaceholder() },
                    error = { PhotoPlaceholder() },
                )
            }

            // Top scrim for distance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            // Bottom scrim for name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.55f),
                            ),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = profile.distance,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Text(
                    text = "$firstName, ${profile.age}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (profile.isOnline) LikeGreen else Color.White.copy(alpha = 0.55f),
                            ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (profile.isOnline) R.string.discover_online_now else R.string.discover_offline,
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        ) {
            Text(
                text = profile.bio.ifBlank { " " },
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.text.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((profile.matchPercentage / 100f).coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(matchColor, RoundedCornerShape(6.dp)),
                    )
                }
                Text(
                    text = matchSentence,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PhotoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(80.dp),
        )
    }
}

@Composable
private fun SwipeStamp(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Black,
        fontSize = 72.sp,
        modifier = modifier,
    )
}

@Composable
private fun DailyLimitCard(
    canUseSecondThoughts: Boolean,
    isLoadingSecondThoughts: Boolean = false,
    errorMessage: String? = null,
    onSecondThoughts: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var showingWhy by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.discover_daily_limit_title),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = colors.text,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.discover_daily_limit_body),
            color = colors.secondaryText,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage, color = LikeRed, fontSize = 13.sp)
        }
        if (canUseSecondThoughts) {
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoadingSecondThoughts) {
                androidx.compose.material3.CircularProgressIndicator(color = DistanceAccent)
            } else {
                TextButton(onClick = onSecondThoughts) {
                    Text(stringResource(R.string.discover_second_thoughts), color = DistanceAccent)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { showingWhy = true }) {
            Text(
                stringResource(R.string.discover_daily_limit_why),
                color = colors.secondaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }

    if (showingWhy) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showingWhy = false },
            title = {
                Text(stringResource(R.string.discover_daily_limit_why_title))
            },
            text = {
                Text(stringResource(R.string.discover_daily_limit_why_message))
            },
            confirmButton = {
                TextButton(onClick = { showingWhy = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

@Composable
private fun IncompleteProfileCard(onFinishProfile: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.discover_incomplete_title),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = colors.text,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.discover_incomplete_body),
            color = colors.secondaryText,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onFinishProfile) {
            Text(stringResource(R.string.discover_incomplete_cta), color = DistanceAccent)
        }
    }
}

@Composable
private fun EmptyDeckCard(modifier: Modifier = Modifier) {
    val colors = LocalSpectRoomColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(24.dp),
    ) {
        Text(
            stringResource(R.string.discover_empty),
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.discover_empty_hint), color = colors.secondaryText)
    }
}

@Composable
private fun ActionCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
) {
    val colors = LocalSpectRoomColors.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(size)
            .background(colors.card, CircleShape)
            .border(1.dp, colors.border, CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(iconSize),
        )
    }
}
