package com.example.spectroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.ui.navigation.MainTab
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.PassRed
import com.example.spectroom.ui.theme.TabSelected

private val BarTopCorner = 15.dp
private val BarHorizontalPadding = 15.dp
private val BarTopPadding = 12.dp
private val CenterButtonSize = 56.dp
private val CenterIconSize = 30.dp
private val CenterButtonLift = 28.dp
private val CenterSlotWidth = 60.dp

/** Tab bar body (icons + labels) — used to reserve list space above the menu. */
val TabBarBodyHeight = 42.dp

/** Main content bottom inset: tab bar body + center button protrusion above the bar. */
val TabBarMainContentInset = TabBarBodyHeight + CenterButtonLift

@Composable
fun SpectRoomBottomBar(
    selectedTab: Int,
    unreadCount: Int,
    matchesCount: Int = 0,
    onTabSelected: (Int) -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val barShape = RoundedCornerShape(topStart = BarTopCorner, topEnd = BarTopCorner)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = CenterButtonLift),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = barShape,
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.1f),
                )
                .clip(barShape)
                .background(colors.card),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BarHorizontalPadding)
                    .padding(top = BarTopPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomTabItem(
                    tab = MainTab.MESSAGES,
                    selected = selectedTab == MainTab.MESSAGES.index,
                    badgeCount = unreadCount,
                    onClick = { onTabSelected(MainTab.MESSAGES.index) },
                    modifier = Modifier.weight(1f),
                )
                BottomTabItem(
                    tab = MainTab.MATCHES,
                    selected = selectedTab == MainTab.MATCHES.index,
                    badgeCount = matchesCount,
                    onClick = { onTabSelected(MainTab.MATCHES.index) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(CenterSlotWidth))
                BottomTabItem(
                    tab = MainTab.PROFILE,
                    selected = selectedTab == MainTab.PROFILE.index,
                    badgeCount = 0,
                    onClick = {
                        onTabSelected(
                            if (selectedTab == MainTab.PROFILE.index) {
                                MainTab.HOME.index
                            } else {
                                MainTab.PROFILE.index
                            },
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                BottomTabItem(
                    tab = MainTab.SETTINGS,
                    selected = selectedTab == MainTab.SETTINGS.index,
                    badgeCount = 0,
                    onClick = { onTabSelected(MainTab.SETTINGS.index) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -CenterButtonLift)
                .zIndex(1f),
        ) {
            val discoverSelected = selectedTab == MainTab.HOME.index
            Box(
                modifier = Modifier
                    .size(CenterButtonSize)
                    .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f))
                    .clip(CircleShape)
                    .background(colors.card)
                    .border(1.dp, colors.floatingPillBorder, CircleShape)
                    .clickable { onTabSelected(MainTab.HOME.index) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile_card),
                    contentDescription = stringResource(R.string.tab_home),
                    tint = if (discoverSelected) TabSelected else colors.secondaryText,
                    modifier = Modifier.size(CenterIconSize),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: MainTab,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    val label = stringResource(tab.labelRes)
    val tint = if (selected) TabSelected else colors.secondaryText

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box {
            Icon(
                imageVector = tab.icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            if (badgeCount > 0) {
                TabBadge(count = badgeCount)
            }
        }
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun BoxScope.TabBadge(count: Int) {
    val label = if (count > 9) "9+" else count.toString()
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 6.dp, y = (-6).dp)
            .size(18.dp)
            .background(PassRed, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

private val MainTab.icon: ImageVector
    get() = when (this) {
        MainTab.MESSAGES -> Icons.Filled.ChatBubble
        MainTab.MATCHES -> Icons.Filled.Favorite
        MainTab.HOME -> Icons.Filled.Favorite // unused — center uses profile card asset
        MainTab.PROFILE -> Icons.Filled.Person
        MainTab.SETTINGS -> Icons.Filled.Settings
    }
