package com.example.spectroom.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.BlockedUser
import com.example.spectroom.repository.ModerationRepository
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import java.text.DateFormat

@Composable
fun BlockedUsersSheet(
    blockedUsers: List<BlockedUser>,
    onUnblock: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var pendingUnblock by remember { mutableStateOf<BlockedUser?>(null) }

    SettingsFullScreenSheet(onDismiss = onDismiss) {
            Column(modifier = Modifier.settingsSheetSurface()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_blocked_users_modal_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 72.dp)
                            .align(Alignment.Center),
                    )
                    Text(
                        text = stringResource(R.string.settings_done),
                        color = colors.text,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, colors.border, RoundedCornerShape(50))
                            .background(colors.card)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }

                if (blockedUsers.isEmpty()) {
                    BlockedUsersEmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        items(blockedUsers, key = { it.id }) { user ->
                            BlockedUserRow(
                                blocked = user,
                                onRequestUnblock = { pendingUnblock = user },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }

            pendingUnblock?.let { user ->
                UnblockConfirmationOverlay(
                    firstName = firstName(user.name),
                    onCancel = { pendingUnblock = null },
                    onConfirm = {
                        onUnblock(user.id)
                        pendingUnblock = null
                    },
                )
            }
    }
}

@Composable
private fun BlockedUsersEmptyState() {
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
                .shadow(12.dp, CircleShape, spotColor = Color.Red.copy(alpha = 0.14f))
                .background(Color.Red.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, Color.Red.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🚫", fontSize = 48.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_blocked_users_empty),
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_blocked_users_empty_description),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlockedUserRow(
    blocked: BlockedUser,
    onRequestUnblock: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val repository = remember { ModerationRepository() }
    var photoUrl by remember(blocked.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(blocked.id) {
        photoUrl = runCatching { repository.fetchProfilePhotoUrl(blocked.id) }.getOrNull()
    }

    val dateText = blocked.blockedAt?.let { blockedAt ->
        stringResource(
            R.string.settings_blocked_users_since,
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(blockedAt),
        )
    } ?: stringResource(R.string.settings_blocked_users_no_date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BlockedUserAvatar(
            name = blocked.name,
            photoUrl = photoUrl,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = blocked.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = colors.text,
            )
            Text(
                text = dateText,
                color = colors.secondaryText,
                fontSize = 12.sp,
            )
        }
        TextButton(onClick = onRequestUnblock) {
            Text(
                text = stringResource(R.string.settings_blocked_users_unblock),
                color = LikeGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun BlockedUserAvatar(
    name: String,
    photoUrl: String?,
) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TabSelected.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = TabSelected,
            )
        }
    }
}

@Composable
fun UnblockConfirmationOverlay(
    firstName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_blocked_users_unblock_confirm_title, firstName),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = stringResource(R.string.settings_blocked_users_unblock_confirm_message, firstName),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                )
            }
            HorizontalDivider(color = colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = colors.text,
                        fontSize = 16.sp,
                    )
                }
                VerticalDivider(modifier = Modifier.height(44.dp), color = colors.border)
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.settings_blocked_users_unblock),
                        color = LikeGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

private fun firstName(fullName: String): String {
    val trimmed = fullName.trim()
    return trimmed.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: trimmed
}
