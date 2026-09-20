package com.example.spectroom.ui.screens.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.AdminSender
import com.example.spectroom.data.model.ConversationDisplay
import com.example.spectroom.ui.components.BlockedProfileLabel
import com.example.spectroom.ui.components.TabBarBodyHeight
import com.example.spectroom.ui.components.UnreadCountBadge
import com.example.spectroom.ui.screens.settings.UnblockConfirmationOverlay
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ListPadding = 16.dp
private val RowSpacing = 8.dp
private val TabMenuReserve = TabBarBodyHeight
private val MessagesTitleTopPadding = 12.dp
private val MessagesTitleLineHeight = 40.dp
private val MessagesSearchTopGap = 4.dp
private val MessagesSearchBottomGap = 10.dp
private val MessagesTitleBarReserve =
    MessagesTitleTopPadding + MessagesTitleLineHeight + MessagesSearchTopGap

@Composable
fun MessagesScreen(
    conversations: List<ConversationDisplay>,
    isLoading: Boolean = false,
    hasMatches: Boolean = false,
    historyMatchIds: Set<String> = emptySet(),
    blockedUserIds: Set<String> = emptySet(),
    blockedAtForUser: (String) -> Date? = { null },
    onSearchQueryChange: (String) -> Unit = {},
    onOpenConversation: (ConversationDisplay) -> Unit,
    onUnblockUser: (String) -> Unit = {},
    onViewMatches: () -> Unit = {},
    onExploreProfiles: () -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    var query by remember { mutableStateOf("") }
    var pendingUnblock by remember { mutableStateOf<ConversationDisplay?>(null) }
    LaunchedEffect(query) { onSearchQueryChange(query) }
    val filtered = remember(conversations, query, historyMatchIds) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) conversations
        else conversations.filter { conversation ->
            conversation.otherUserName.lowercase().contains(needle) ||
                conversation.lastMessage.lowercase().contains(needle) ||
                conversation.otherUserId in historyMatchIds
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Spacer(modifier = Modifier.height(MessagesTitleBarReserve))

            if (!isLoading && conversations.isNotEmpty()) {
                MessagesSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier
                        .padding(horizontal = ListPadding)
                        .padding(bottom = MessagesSearchBottomGap),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    isLoading -> MessagesLoadingView()
                    conversations.isEmpty() -> MessagesEmptyView(
                        hasMatches = hasMatches,
                        onViewMatches = onViewMatches,
                        onExploreProfiles = onExploreProfiles,
                    )
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = ListPadding,
                                end = ListPadding,
                                top = 8.dp,
                                bottom = 16.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(RowSpacing),
                        ) {
                            items(filtered, key = { it.id }) { conversation ->
                                ConversationRow(
                                    conversation = conversation,
                                    blockedByMe = conversation.otherUserId in blockedUserIds,
                                    blockedAt = blockedAtForUser(conversation.otherUserId),
                                    onClick = { onOpenConversation(conversation) },
                                    onRequestUnblock = { pendingUnblock = conversation },
                                )
                            }
                        }
                        if (filtered.isEmpty()) {
                            MessagesNoSearchResultsView()
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
                .padding(top = MessagesTitleTopPadding, bottom = MessagesSearchTopGap),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.messages_title),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.text,
            )
        }

        pendingUnblock?.let { conversation ->
            UnblockConfirmationOverlay(
                firstName = conversation.otherUserName
                    .substringBefore(' ')
                    .ifBlank { conversation.otherUserName },
                onCancel = { pendingUnblock = null },
                onConfirm = {
                    onUnblockUser(conversation.otherUserId)
                    pendingUnblock = null
                },
            )
        }
    }
}

@Composable
private fun MessagesSearchBar(
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(16.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 20.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = colors.text,
            ),
            singleLine = true,
            cursorBrush = SolidColor(TabSelected),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.messages_search_placeholder),
                            color = colors.secondaryText,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.messages_search_clear),
                tint = colors.secondaryText,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun MessagesLoadingView() {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = TabSelected, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(stringResource(R.string.messages_loading), color = colors.secondaryText, fontSize = 15.sp)
    }
}

@Composable
private fun MessagesEmptyView(
    hasMatches: Boolean,
    onViewMatches: () -> Unit,
    onExploreProfiles: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = TabSelected.copy(alpha = 0.16f),
                    spotColor = TabSelected.copy(alpha = 0.16f),
                )
                .clip(CircleShape)
                .background(TabSelected.copy(alpha = 0.14f))
                .border(1.dp, TabSelected.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "💬",
                fontSize = 48.sp,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.messages_empty_has_matches_title),
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                if (hasMatches) {
                    R.string.messages_empty_has_matches_description
                } else {
                    R.string.messages_empty_no_matches_description
                },
            ),
            fontSize = 15.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = if (hasMatches) onViewMatches else onExploreProfiles,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
        ) {
            Text(
                text = stringResource(
                    if (hasMatches) {
                        R.string.messages_empty_has_matches_button
                    } else {
                        R.string.messages_empty_no_matches_button
                    },
                ),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MessagesNoSearchResultsView() {
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
            text = stringResource(R.string.messages_search_no_results_hint),
            fontSize = 15.sp,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationDisplay,
    blockedByMe: Boolean,
    blockedAt: Date?,
    onClick: () -> Unit,
    onRequestUnblock: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val firstName = conversation.otherUserName.substringBefore(' ').ifBlank { conversation.otherUserName }
    val timeText = formatConversationListTime(conversation.lastMessageAt)
    val blockedDateText = blockedAt?.let { at ->
        stringResource(
            R.string.settings_blocked_users_since,
            java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.MEDIUM,
                java.text.DateFormat.SHORT,
            ).format(at),
        )
    } ?: stringResource(R.string.settings_blocked_users_no_date)

    if (blockedByMe) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card.copy(alpha = 0.55f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.28f)
                        .clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ConversationAvatar(conversation = conversation, firstName = firstName)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = conversation.otherUserName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = blockedDateText,
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_blocked_users_unblock),
                    color = LikeGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onRequestUnblock),
                )
            }
            BlockedProfileLabel(
                dimmed = false,
                slanted = false,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(50.dp)) {
            ConversationAvatar(conversation = conversation, firstName = firstName)
            if (conversation.unreadCount > 0) {
                UnreadCountBadge(count = conversation.unreadCount)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = conversation.otherUserName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (conversation.lastMessageDeleted) {
                    stringResource(R.string.messages_deleted_placeholder)
                } else {
                    conversation.lastMessage
                },
                color = colors.secondaryText,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = timeText,
            color = colors.text.copy(alpha = 0.75f),
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun ConversationAvatar(
    conversation: ConversationDisplay,
    firstName: String,
) {
    if (AdminSender.isAdmin(conversation.otherUserId)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(AdminSender.photoRes),
                contentDescription = conversation.otherUserName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
            )
        }
    } else if (conversation.otherUserPhotoURL.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.1f))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TabSelected.copy(0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = firstName.take(1).uppercase(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = TabSelected,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.1f))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = conversation.otherUserPhotoURL,
                contentDescription = conversation.otherUserName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun formatConversationListTime(date: Date): String {
    val messageCal = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    if (messageCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        messageCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    ) {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (messageCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        messageCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    ) {
        return stringResource(R.string.messages_date_yesterday)
    }
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
}
