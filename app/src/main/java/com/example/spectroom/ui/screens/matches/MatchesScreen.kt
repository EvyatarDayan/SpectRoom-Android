package com.example.spectroom.ui.screens.matches

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.EnergyLevel
import com.example.spectroom.data.model.MatchDisplay
import com.example.spectroom.ui.components.TabBarBodyHeight
import com.example.spectroom.ui.theme.EnergyFullySocial
import com.example.spectroom.ui.theme.EnergyLow
import com.example.spectroom.ui.theme.EnergyModerate
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

private val ListPadding = 16.dp
private val RowSpacing = 14.dp
private val TabMenuReserve = TabBarBodyHeight
/** Reserves the same vertical space the title used to occupy so content position stays fixed. */
private val MatchesTitleBarReserve = 48.dp

@Composable
fun MatchesScreen(
    matches: List<MatchDisplay>,
    isLoading: Boolean = false,
    isBlocked: (String) -> Boolean = { false },
    onOpenMatch: (MatchDisplay) -> Unit,
    onMessageMatch: (MatchDisplay) -> Unit = {},
    onExploreProfiles: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(matches, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) matches
        else matches.filter { match ->
            match.otherUser.name.lowercase().contains(needle) ||
                match.otherUser.interests.any { it.lowercase().contains(needle) } ||
                match.otherUser.bio.lowercase().contains(needle)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(MatchesTitleBarReserve))

            if (!isLoading && matches.isNotEmpty()) {
                MatchesSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier
                        .padding(horizontal = ListPadding)
                        .padding(top = 4.dp, bottom = 10.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    isLoading -> MatchesLoadingView()
                    matches.isEmpty() -> MatchesEmptyView(onExploreProfiles = onExploreProfiles)
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(RowSpacing),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = ListPadding,
                                end = ListPadding,
                                top = 8.dp,
                                bottom = 16.dp,
                            ),
                        ) {
                            items(filtered, key = { it.id }) { match ->
                                MatchGridCard(
                                    match = match,
                                    blocked = isBlocked(match.otherUser.userId),
                                    onTap = { onOpenMatch(match) },
                                    onMessage = { onMessageMatch(match) },
                                )
                            }
                        }
                        if (filtered.isEmpty()) {
                            MatchesNoSearchResultsView()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(TabMenuReserve))
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.matches_title),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.text,
            )
        }
    }
}

@Composable
private fun MatchesEmptyHeartsIcon() {
    val heartPink = Color(0xFFFF5C8A)
    val heartPinkLight = Color(0xFFFF8CB0)
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFFF4081).copy(0.18f),
                    spotColor = Color(0xFFFF4081).copy(0.18f),
                )
                .clip(CircleShape)
                .background(Color(0xFFFF4081).copy(0.16f))
                .border(1.dp, Color(0xFFFF4081).copy(0.22f), CircleShape),
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = heartPinkLight,
            modifier = Modifier
                .size(34.dp)
                .offset(x = (-10).dp, y = 4.dp),
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = heartPink,
            modifier = Modifier
                .size(42.dp)
                .offset(x = 8.dp, y = (-2).dp),
        )
    }
}

@Composable
private fun MatchesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(0.08f))
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(18.dp),
        )
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    stringResource(R.string.matches_search_placeholder),
                    color = colors.secondaryText,
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = TabSelected,
            ),
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.matches_search_clear),
                tint = colors.secondaryText,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun MatchesLoadingView() {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = TabSelected,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.matches_loading),
            color = colors.secondaryText,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun MatchesEmptyView(onExploreProfiles: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MatchesEmptyHeartsIcon()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.matches_empty),
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.matches_empty_description),
            fontSize = 15.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onExploreProfiles,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
        ) {
            Text(
                text = stringResource(R.string.matches_explore_profiles),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MatchesNoSearchResultsView() {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.92f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = TabSelected.copy(alpha = 0.75f),
                modifier = Modifier.size(64.dp),
            )
            Box(
                modifier = Modifier
                    .offset(x = 6.dp, y = 6.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.card),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.messages_search_no_results),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = colors.text.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.matches_search_no_results_hint),
            fontSize = 15.sp,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}

@Composable
private fun MatchGridCard(
    match: MatchDisplay,
    blocked: Boolean,
    onTap: () -> Unit,
    onMessage: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val configuration = LocalConfiguration.current
    val cardWidth = configuration.screenWidthDp.dp - (ListPadding * 2)
    val cardPadding = 10.dp
    val contentSpacing = 12.dp
    val photoSide = (cardWidth - (cardPadding * 2) - contentSpacing) / 2
    val profile = match.otherUser
    val firstName = profile.name.substringBefore(' ').ifBlank { profile.name }
    val distance = profile.distance.trim()
    val showDistance = distance.isNotEmpty() && !distance.equals("Matched", ignoreCase = true)
    val energyColor = when (EnergyLevel.resolve(profile.energyLevel)) {
        EnergyLevel.LOW -> EnergyLow
        EnergyLevel.MODERATE -> EnergyModerate
        else -> EnergyFullySocial
    }
    val photoBorder = if (colors.isDark) Color.White.copy(0.28f) else Color.White.copy(0.9f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            horizontalArrangement = Arrangement.spacedBy(contentSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Box(
            modifier = Modifier.size(photoSide),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, photoBorder, RoundedCornerShape(14.dp))
                    .clickable(onClick = onTap),
            ) {
                if (profile.imageURL.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                } else {
                    AsyncImage(
                        model = profile.imageURL,
                        contentDescription = profile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (!blocked) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TabSelected)
                        .clickable(onClick = onMessage)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(R.string.matches_message_button),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .width(photoSide)
                .height(photoSide)
                .clickable(onClick = onTap),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$firstName, ${profile.age}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                if (showDistance) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = distance,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        if (colors.isDark) Color.White.copy(0.22f) else Color(0xFFB8B8B8).copy(0.4f),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = EnergyLevel.emoji(profile.energyLevel),
                    fontSize = 18.sp,
                    modifier = Modifier.offset(x = 12.dp),
                )
                Text(
                    text = stringResource(
                        R.string.matches_energy_today,
                        EnergyLevel.label(profile.energyLevel),
                    ),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = energyColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 6.dp, end = 18.dp),
                )
            }
        }
        }
        if (blocked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.messages_conversation_blocked),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LikeRed.copy(0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
