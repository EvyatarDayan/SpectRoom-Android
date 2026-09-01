package com.example.spectroom.ui.screens.discover

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.spectroom.ui.theme.EnergyLow
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.PassRed
import com.example.spectroom.ui.theme.TabSelected
import com.example.spectroom.viewmodel.DiscoverUiState
import kotlin.math.roundToInt

private val CardCorner = 24.dp
private val PhotoHeight = 340.dp
private val CardHeight = 500.dp
private val CardHorizontalInset = 30.dp
private val CardAreaTopPadding = 150.dp
private val CardVerticalOffset = 36.dp
private val EnergyPickerContentPadding = 24.dp
private val EnergyPickerHeaderHeight = 78.dp
private val EnergyPickerSectionSpacing = 12.dp
private val EnergyPickerRowHeight = 112.dp
private val EnergyPickerPanelHeight =
    EnergyPickerContentPadding * 2 +
        EnergyPickerHeaderHeight +
        EnergyPickerSectionSpacing +
        EnergyPickerRowHeight * 3 +
        EnergyPickerSectionSpacing * 2
private val DiscoverDialogOptionsStackHeight =
    EnergyPickerRowHeight * 3 + EnergyPickerSectionSpacing * 2
private val DistancePickerRowHeight =
    (DiscoverDialogOptionsStackHeight - EnergyPickerSectionSpacing * 4) / 5
private val PickerPanelTopOrigin = TransformOrigin(0.5f, 0f)
private val PickerPanelOpenSpring = spring<Float>(dampingRatio = 0.78f, stiffness = 220f)
private val PickerPanelCloseSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 340f)

private fun pickerPanelEnterTransition() =
    fadeIn(animationSpec = PickerPanelOpenSpring) +
        scaleIn(
            initialScale = 0.82f,
            transformOrigin = PickerPanelTopOrigin,
            animationSpec = PickerPanelOpenSpring,
        )

private fun pickerPanelExitTransition() =
    fadeOut(animationSpec = PickerPanelCloseSpring) +
        scaleOut(
            targetScale = 0.82f,
            transformOrigin = PickerPanelTopOrigin,
            animationSpec = PickerPanelCloseSpring,
        )
private val ActionRowBottomPadding = TabBarBodyHeight
private val CardAreaBottomPadding = 108.dp
private val SwipeStampYOffset = 52.dp

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    currentEnergyLevel: Int = EnergyLevel.FULLY_SOCIAL,
    profileIncomplete: Boolean = false,
    onSwipe: (SwipeType) -> Unit,
    onProgrammaticSwipe: (SwipeType) -> Unit = {},
    onProgrammaticSwipeComplete: (SwipeType) -> Unit = {},
    onUndo: () -> Unit,
    onSecondThoughts: () -> Unit,
    onOpenProfile: (ProfileCard) -> Unit = {},
    onUpdateEnergyLevel: (Int) -> Unit = {},
    onSetDistance: (DiscoverDistanceOption) -> Unit = {},
    onFinishProfile: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val reachedLimit = state.dailySwipeCount >= DiscoverDailyLimitStore.MAX_SWIPES_PER_DAY &&
        !state.isReviewingTodaysPasses
    val frozenCardIndex = when {
        state.profiles.isEmpty() -> -1
        reachedLimit -> minOf(state.currentIndex, state.profiles.lastIndex).coerceAtLeast(0)
        else -> state.currentIndex
    }
    val profile = state.profiles.getOrNull(frozenCardIndex)
    var showingEnergyPicker by remember { mutableStateOf(false) }
    var showingDistancePicker by remember { mutableStateOf(false) }
    var showingDailyLimitWhy by remember { mutableStateOf(false) }
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
                .offset(y = (-CardVerticalOffset)),
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
                profile == null -> EmptyDeckCard(modifier = Modifier.align(Alignment.Center))
                else -> {
                    val start = frozenCardIndex
                    val end = minOf(start + 3, state.profiles.size)
                    for (index in (end - 1) downTo start) {
                        val depth = index - start
                        val stacked = state.profiles[index]
                        SwipeableProfileCard(
                            profile = stacked,
                            depth = depth,
                            isTopCard = depth == 0,
                            programmaticSwipe = if (depth == 0) state.programmaticSwipe else null,
                            isDailyLimitReached = reachedLimit,
                            canUseSecondThoughts = !state.hasUsedSecondThoughtsToday,
                            isLoadingSecondThoughts = state.isLoadingSecondThoughts,
                            secondThoughtsError = state.secondThoughtsError,
                            onSecondThoughts = onSecondThoughts,
                            onShowDailyLimitWhy = { showingDailyLimitWhy = true },
                            onSwiped = onSwipe,
                            onProgrammaticSwipeComplete = onProgrammaticSwipeComplete,
                            onOpen = {
                                if (!reachedLimit) onOpenProfile(stacked)
                            },
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
                    onClick = { onProgrammaticSwipe(SwipeType.PASS) },
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
                    onClick = { onProgrammaticSwipe(SwipeType.LIKE) },
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

        EnergyLevelPickerOverlay(
            visible = showingEnergyPicker,
            selectedLevel = currentEnergyLevel,
            onSelect = { level ->
                onUpdateEnergyLevel(level)
                showingEnergyPicker = false
            },
            onDismiss = { showingEnergyPicker = false },
        )

        DistancePickerOverlay(
            visible = showingDistancePicker,
            selected = state.distanceOption,
            onSelect = { option ->
                onSetDistance(option)
                showingDistancePicker = false
            },
            onDismiss = { showingDistancePicker = false },
        )

        if (showingDailyLimitWhy) {
            DailyLimitWhyOverlay(onDismiss = { showingDailyLimitWhy = false })
        }
    }
}

@Composable
private fun DiscoverPickerOverlayHost(
    visible: Boolean,
    zIndex: Float,
    onDismiss: () -> Unit,
    panel: @Composable () -> Unit,
) {
    val dismissInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(zIndex),
    ) {
        if (visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = CardHorizontalInset,
                    end = CardHorizontalInset,
                    top = CardAreaTopPadding,
                    bottom = CardAreaBottomPadding,
                )
                .offset(y = (-CardVerticalOffset)),
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = pickerPanelEnterTransition(),
                exit = pickerPanelExitTransition(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = CardHeight - EnergyPickerPanelHeight),
            ) {
                panel()
            }
        }
    }
}

@Composable
private fun DistancePickerOverlay(
    visible: Boolean,
    selected: DiscoverDistanceOption,
    onSelect: (DiscoverDistanceOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val accent = EnergyLow
    val panelShape = RoundedCornerShape(24.dp)
    val rowShape = RoundedCornerShape(16.dp)
    val panelBackground = if (colors.isDark) Color.Black else Color.White
    val primaryText = if (colors.isDark) Color.White else Color.Black
    val secondaryText = if (colors.isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.55f)

    DiscoverPickerOverlayHost(
        visible = visible,
        zIndex = 201f,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(EnergyPickerPanelHeight)
                .shadow(
                    elevation = 16.dp,
                    shape = panelShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .clip(panelShape)
                .background(panelBackground)
                .border(
                    width = 1.dp,
                    color = if (colors.isDark) Color.Transparent else Color.Black.copy(alpha = 0.06f),
                    shape = panelShape,
                )
                .clickable(enabled = false) {}
                .padding(EnergyPickerContentPadding),
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EnergyPickerHeaderHeight),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Text(
                        text = stringResource(R.string.discover_distance_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = primaryText,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.discover_distance_subtitle),
                        color = secondaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 21.sp,
                    )
                }
                Spacer(modifier = Modifier.height(EnergyPickerSectionSpacing))
                Column(verticalArrangement = Arrangement.spacedBy(EnergyPickerSectionSpacing)) {
                    DiscoverDistanceOption.entries.forEach { option ->
                        val isSelected = option == selected
                        val iconColor = if (isSelected) {
                            accent
                        } else if (colors.isDark) {
                            Color.White.copy(alpha = 0.45f)
                        } else {
                            Color.Black.copy(alpha = 0.45f)
                        }
                        val fill = if (isSelected) {
                            accent.copy(alpha = if (colors.isDark) 0.28f else 0.18f)
                        } else if (colors.isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.Black.copy(alpha = 0.05f)
                        }
                        val stroke = if (isSelected) {
                            accent.copy(alpha = 0.95f)
                        } else if (colors.isDark) {
                            Color.White.copy(alpha = 0.18f)
                        } else {
                            Color.Black.copy(alpha = 0.14f)
                        }
                        val titleColor = if (isSelected) primaryText else primaryText.copy(alpha = 0.75f)
                        val rowInteraction = remember(option) { MutableInteractionSource() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DistancePickerRowHeight)
                                .clip(rowShape)
                                .background(fill)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = stroke,
                                    shape = rowShape,
                                )
                                .clickable(
                                    interactionSource = rowInteraction,
                                    indication = null,
                                    onClick = { onSelect(option) },
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(option.iconRes()),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    colorFilter = ColorFilter.tint(iconColor),
                                )
                            }
                            if (option == DiscoverDistanceOption.UNLIMITED) {
                                Text(
                                    text = stringResource(option.titleRes()),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = titleColor,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Text(
                                    text = stringResource(option.titleRes()),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = titleColor,
                                    modifier = Modifier.width(64.dp),
                                )
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    DistanceIllustrationBar(
                                        option = option,
                                        color = iconColor,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun DistanceIllustrationBar(
    option: DiscoverDistanceOption,
    color: Color,
) {
    val barWidth = option.illustrationBarWidthDp?.dp ?: return
    val tickHeight = 9.dp
    val tickWidth = 2.dp
    Box(
        modifier = Modifier
            .width(barWidth)
            .height(tickHeight),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(barWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(tickWidth, tickHeight)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = barWidth - tickWidth)
                .size(tickWidth, tickHeight)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
private fun DailyLimitWhyOverlay(onDismiss: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    val dismissInteraction = remember { MutableInteractionSource() }
    val okInteraction = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(28.dp)
    val cardBackground = if (colors.isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.82f)
    }
    val buttonBackground = if (colors.isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = dismissInteraction,
                indication = null,
                onClick = onDismiss,
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardBackground)
                .border(
                    width = 0.5.dp,
                    color = if (colors.isDark) {
                        Color.White.copy(alpha = 0.14f)
                    } else {
                        Color.White.copy(alpha = 0.65f)
                    },
                    shape = cardShape,
                )
                .clickable(enabled = false) {}
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.discover_daily_limit_why_title),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.discover_daily_limit_why_message),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(buttonBackground)
                    .clickable(
                        interactionSource = okInteraction,
                        indication = null,
                        onClick = onDismiss,
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.common_ok),
                    color = colors.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun EnergyLevelPickerOverlay(
    visible: Boolean,
    selectedLevel: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val accent = EnergyLow
    val panelShape = RoundedCornerShape(24.dp)
    val rowShape = RoundedCornerShape(16.dp)
    val panelBackground = if (colors.isDark) Color.Black else Color.White
    val primaryText = if (colors.isDark) Color.White else Color.Black
    val secondaryText = if (colors.isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.55f)

    DiscoverPickerOverlayHost(
        visible = visible,
        zIndex = 200f,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(EnergyPickerPanelHeight)
                .shadow(
                    elevation = 16.dp,
                    shape = panelShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .clip(panelShape)
                .background(panelBackground)
                .border(
                    width = 1.dp,
                    color = if (colors.isDark) Color.Transparent else Color.Black.copy(alpha = 0.06f),
                    shape = panelShape,
                )
                .clickable(enabled = false) {}
                .padding(EnergyPickerContentPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EnergyPickerHeaderHeight),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = stringResource(R.string.profile_my_energy_level),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = primaryText,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.profile_energy_level_subtitle),
                    color = secondaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                )
            }
            Spacer(modifier = Modifier.height(EnergyPickerSectionSpacing))
            Column(verticalArrangement = Arrangement.spacedBy(EnergyPickerSectionSpacing)) {
                EnergyLevel.all.forEach { level ->
                    val selected = EnergyLevel.resolve(selectedLevel) == level
                    val fill = if (selected) {
                        accent.copy(alpha = if (colors.isDark) 0.28f else 0.18f)
                    } else if (colors.isDark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.Black.copy(alpha = 0.05f)
                    }
                    val stroke = if (selected) {
                        accent.copy(alpha = 0.95f)
                    } else if (colors.isDark) {
                        Color.White.copy(alpha = 0.18f)
                    } else {
                        Color.Black.copy(alpha = 0.14f)
                    }
                    val titleColor = if (selected) primaryText else primaryText.copy(alpha = 0.75f)
                    val detailColor = if (selected) secondaryText else secondaryText.copy(alpha = 0.85f)
                    val rowInteraction = remember(level) { MutableInteractionSource() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(EnergyPickerRowHeight)
                            .clip(rowShape)
                            .background(fill)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = stroke,
                                shape = rowShape,
                            )
                            .clickable(
                                interactionSource = rowInteraction,
                                indication = null,
                                onClick = { onSelect(level) },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(EnergyLevel.emoji(level), fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(EnergyLevel.titleRes(level)),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = titleColor,
                            )
                            Text(
                                text = stringResource(EnergyLevel.detailRes(level)),
                                color = detailColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp,
                            )
                        }
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
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
    programmaticSwipe: SwipeType? = null,
    isDailyLimitReached: Boolean = false,
    canUseSecondThoughts: Boolean = false,
    isLoadingSecondThoughts: Boolean = false,
    secondThoughtsError: String? = null,
    onSecondThoughts: () -> Unit = {},
    onShowDailyLimitWhy: () -> Unit = {},
    onSwiped: (SwipeType) -> Unit,
    onProgrammaticSwipeComplete: (SwipeType) -> Unit = {},
    onOpen: () -> Unit,
) {
    var offsetX by remember(profile.userId) { mutableFloatStateOf(0f) }
    var offsetY by remember(profile.userId) { mutableFloatStateOf(0f) }
    var isAnimatingProgrammaticSwipe by remember(profile.userId) { mutableStateOf(false) }
    val rotation = if (isTopCard && !isDailyLimitReached) offsetX / 20f else 0f
    val density = LocalDensity.current
    val threshold = with(density) { 100.dp.toPx() }
    val handleGestures = rememberUpdatedState(isTopCard && !isDailyLimitReached && !isAnimatingProgrammaticSwipe)

    LaunchedEffect(programmaticSwipe) {
        val action = programmaticSwipe ?: return@LaunchedEffect
        if (!isTopCard || isDailyLimitReached) return@LaunchedEffect
        isAnimatingProgrammaticSwipe = true
        val stampOffset = with(density) { 170.dp.toPx() }
        val offScreen = with(density) { 1000.dp.toPx() }
        val toward = when (action) {
            SwipeType.PASS -> -1f
            SwipeType.LIKE, SwipeType.SUPER_LIKE -> 1f
        }
        animate(
            initialValue = offsetX,
            targetValue = stampOffset * toward,
            animationSpec = tween(durationMillis = 600),
        ) { value, _ -> offsetX = value }
        animate(
            initialValue = offsetX,
            targetValue = offScreen * toward,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        ) { value, _ -> offsetX = value }
        onProgrammaticSwipeComplete(action)
        isAnimatingProgrammaticSwipe = false
    }

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
                alpha = if (isDailyLimitReached) 0.85f else stackAlpha
            }
            .offset {
                IntOffset(
                    if (isTopCard && !isDailyLimitReached) offsetX.roundToInt() else 0,
                    if (isTopCard && !isDailyLimitReached) offsetY.roundToInt() else 0,
                )
            }
            .rotate(rotation)
            .then(
                if (!isDailyLimitReached) {
                    Modifier.pointerInput(profile.userId, isAnimatingProgrammaticSwipe) {
                        // Attach gestures to every visible stacked card so the next card is ready
                        // before it becomes top (avoids losing the first tap/swipe after promotion).
                        awaitEachGesture {
                            if (!handleGestures.value) return@awaitEachGesture
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDrag = Offset.Zero
                            var isDragging = false
                            val touchSlop = viewConfiguration.touchSlop
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) continue
                                val delta = change.positionChange()
                                if (!isDragging) {
                                    totalDrag += delta
                                    if (totalDrag.getDistance() > touchSlop) {
                                        isDragging = true
                                    }
                                }
                                if (isDragging) {
                                    change.consume()
                                    offsetX += delta.x
                                    offsetY += delta.y * 0.25f
                                }
                            } while (event.changes.any { it.pressed })
                            if (!isDragging) {
                                onOpen()
                            } else {
                                when {
                                    offsetX > threshold -> onSwiped(SwipeType.LIKE)
                                    offsetX < -threshold -> onSwiped(SwipeType.PASS)
                                    else -> {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isDailyLimitReached) Modifier.blur(8.dp) else Modifier),
        ) {
            ProfileCardContent(profile = profile)
        }

        if (isDailyLimitReached) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(CardCorner))
                    .background(Color.Black.copy(alpha = 0.28f)),
            )
        }

        if (isDailyLimitReached && isTopCard) {
            DailyLimitOverlay(
                canUseSecondThoughts = canUseSecondThoughts,
                isLoadingSecondThoughts = isLoadingSecondThoughts,
                errorMessage = secondThoughtsError,
                onSecondThoughts = onSecondThoughts,
                onShowWhy = onShowDailyLimitWhy,
            )
        }

        if (isTopCard && !isDailyLimitReached) {
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
private fun DailyLimitOverlay(
    canUseSecondThoughts: Boolean,
    isLoadingSecondThoughts: Boolean,
    errorMessage: String?,
    onSecondThoughts: () -> Unit,
    onShowWhy: () -> Unit,
) {
    val whyInteractionSource = remember { MutableInteractionSource() }
    val secondThoughtsInteractionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.discover_daily_limit_title),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (canUseSecondThoughts) {
            Spacer(modifier = Modifier.height(18.dp))
            if (isLoadingSecondThoughts) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.discover_second_thoughts),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(
                            interactionSource = secondThoughtsInteractionSource,
                            indication = null,
                            onClick = onSecondThoughts,
                        )
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(if (canUseSecondThoughts) 2.dp else 0.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.discover_daily_limit_why),
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(
                interactionSource = whyInteractionSource,
                indication = null,
                onClick = onShowWhy,
            ),
        )
    }
}

@Composable
private fun ProfileCardContent(profile: ProfileCard) {
    val colors = LocalSpectRoomColors.current
    val firstName = profile.name.substringBefore(' ').ifBlank { profile.name }
    val photoUrl = profile.imageURL ?: profile.photos.firstOrNull()
    val matchColor = if (profile.matchPercentage > 49) LikeGreen else colors.secondaryText
    val matchSentence = buildMatchSentence(
        percentage = profile.matchPercentage,
        firstName = firstName,
        percentColor = matchColor,
    )

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(CardCorner),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(CardCorner))
            .background(colors.card),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.discover_empty),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
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
