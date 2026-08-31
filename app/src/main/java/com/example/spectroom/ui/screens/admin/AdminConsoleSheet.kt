package com.example.spectroom.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.AdminUserProfile
import com.example.spectroom.data.model.ModerationFreezeDuration
import com.example.spectroom.data.model.UserReport
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import java.text.DateFormat

private enum class AdminTab { REPORTS, USERS }
private enum class ReportFilter { PENDING, ALL, ACTIONED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleSheet(
    reports: List<UserReport>,
    users: List<AdminUserProfile>,
    isLoading: Boolean,
    errorMessage: String?,
    actionMessage: String?,
    onLoad: (search: String) -> Unit,
    onDismissReport: (UserReport) -> Unit,
    onWarn: (userId: String, message: String, reportId: String?) -> Unit,
    onFreeze: (userId: String, duration: ModerationFreezeDuration, reportId: String?) -> Unit,
    onBan: (userId: String, reportId: String?) -> Unit,
    onLiftFreeze: (userId: String) -> Unit = {},
    onLiftBan: (userId: String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(AdminTab.REPORTS) }
    var filter by remember { mutableStateOf(ReportFilter.PENDING) }
    var search by remember { mutableStateOf("") }
    var selectedReport by remember { mutableStateOf<UserReport?>(null) }
    var selectedUser by remember { mutableStateOf<AdminUserProfile?>(null) }

    LaunchedEffect(Unit) { onLoad("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                stringResource(R.string.admin_section_title),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == AdminTab.REPORTS,
                    onClick = { tab = AdminTab.REPORTS },
                    label = { Text(stringResource(R.string.admin_reports_title)) },
                )
                FilterChip(
                    selected = tab == AdminTab.USERS,
                    onClick = { tab = AdminTab.USERS },
                    label = { Text(stringResource(R.string.admin_all_profiles_title)) },
                )
            }
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = LikeRed, fontSize = 13.sp)
            }
            if (!actionMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(actionMessage, color = Color(0xFF2E7D32), fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TabSelected)
                }
            } else when (tab) {
                AdminTab.REPORTS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filter == ReportFilter.PENDING,
                            onClick = { filter = ReportFilter.PENDING },
                            label = { Text(stringResource(R.string.admin_reports_filter_pending)) },
                        )
                        FilterChip(
                            selected = filter == ReportFilter.ALL,
                            onClick = { filter = ReportFilter.ALL },
                            label = { Text(stringResource(R.string.admin_reports_filter_all)) },
                        )
                        FilterChip(
                            selected = filter == ReportFilter.ACTIONED,
                            onClick = { filter = ReportFilter.ACTIONED },
                            label = { Text(stringResource(R.string.admin_reports_filter_actioned)) },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val filtered = reports.filter { report ->
                        when (filter) {
                            ReportFilter.PENDING -> report.status.equals("pending", ignoreCase = true)
                            ReportFilter.ACTIONED ->
                                report.status.equals("actioned", ignoreCase = true) ||
                                    report.status.equals("dismissed", ignoreCase = true)
                            ReportFilter.ALL -> true
                        }
                    }
                    if (filtered.isEmpty()) {
                        EmptyCopy(
                            title = stringResource(R.string.admin_reports_empty),
                            body = stringResource(R.string.admin_reports_empty_description),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filtered, key = { it.id }) { report ->
                                ReportRow(report) { selectedReport = report }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                AdminTab.USERS -> {
                    OutlinedTextField(
                        value = search,
                        onValueChange = {
                            search = it
                            onLoad(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.admin_all_profiles_search_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (users.isEmpty()) {
                        EmptyCopy(
                            title = stringResource(R.string.admin_all_profiles_empty),
                            body = stringResource(R.string.admin_all_profiles_empty_description),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(users, key = { it.id }) { user ->
                                UserRow(user) { selectedUser = user }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_done), color = TabSelected)
            }
        }
    }

    selectedReport?.let { report ->
        RestrictActionsSheet(
            title = report.reportedName,
            subtitle = report.category,
            defaultWarning = stringResource(R.string.admin_warning_default_message),
            onDismissReport = {
                onDismissReport(report)
                selectedReport = null
            },
            onWarn = { message ->
                onWarn(report.reportedUserId, message, report.id)
                selectedReport = null
            },
            onFreeze = { duration ->
                onFreeze(report.reportedUserId, duration, report.id)
                selectedReport = null
            },
            onBan = {
                onBan(report.reportedUserId, report.id)
                selectedReport = null
            },
            onLiftFreeze = {
                onLiftFreeze(report.reportedUserId)
                selectedReport = null
            },
            onLiftBan = {
                onLiftBan(report.reportedUserId)
                selectedReport = null
            },
            onClose = { selectedReport = null },
            showDismissReport = true,
        )
    }

    selectedUser?.let { user ->
        RestrictActionsSheet(
            title = user.titleText,
            subtitle = user.email.ifBlank { user.id },
            defaultWarning = stringResource(R.string.admin_warning_default_message),
            isBanned = user.isBanned,
            isFrozen = user.isFrozen,
            onDismissReport = {},
            onWarn = { message ->
                onWarn(user.id, message, null)
                selectedUser = null
            },
            onFreeze = { duration ->
                onFreeze(user.id, duration, null)
                selectedUser = null
            },
            onBan = {
                onBan(user.id, null)
                selectedUser = null
            },
            onLiftFreeze = {
                onLiftFreeze(user.id)
                selectedUser = null
            },
            onLiftBan = {
                onLiftBan(user.id)
                selectedUser = null
            },
            onClose = { selectedUser = null },
            showDismissReport = false,
        )
    }
}

@Composable
private fun EmptyCopy(title: String, body: String) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = colors.text)
        Spacer(modifier = Modifier.height(8.dp))
        Text(body, color = colors.secondaryText, fontSize = 14.sp)
    }
}

@Composable
private fun ReportRow(report: UserReport, onClick: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    val date = report.createdAt?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)
    }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = report.primaryPhotoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.secondaryText.copy(alpha = 0.2f)),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(report.reportedName, fontWeight = FontWeight.SemiBold, color = colors.text)
            Text(report.category, color = colors.secondaryText, fontSize = 13.sp)
            if (date.isNotEmpty()) {
                Text(date, color = colors.secondaryText, fontSize = 12.sp)
            }
        }
        Text(report.status, color = TabSelected, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UserRow(user: AdminUserProfile, onClick: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    val status = when {
        user.isDeleted -> stringResource(R.string.admin_all_profiles_status_deleted)
        user.isBanned -> stringResource(R.string.admin_all_profiles_status_banned)
        user.isFrozen -> stringResource(R.string.admin_all_profiles_status_frozen)
        user.isOnline -> stringResource(R.string.admin_all_profiles_status_online)
        else -> stringResource(R.string.admin_all_profiles_status_offline)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.secondaryText.copy(alpha = 0.2f)),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.titleText, fontWeight = FontWeight.SemiBold, color = colors.text)
            if (user.email.isNotBlank()) {
                Text(user.email, color = colors.secondaryText, fontSize = 13.sp)
            }
        }
        Text(status, color = TabSelected, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestrictActionsSheet(
    title: String,
    subtitle: String,
    defaultWarning: String,
    showDismissReport: Boolean,
    isBanned: Boolean = false,
    isFrozen: Boolean = false,
    onDismissReport: () -> Unit,
    onWarn: (String) -> Unit,
    onFreeze: (ModerationFreezeDuration) -> Unit,
    onBan: () -> Unit,
    onLiftFreeze: () -> Unit = {},
    onLiftBan: () -> Unit = {},
    onClose: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var warningText by remember { mutableStateOf(defaultWarning) }
    var confirmBan by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.text)
            Text(subtitle, color = colors.secondaryText, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.admin_all_profiles_restrict_title),
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isBanned) {
                ActionButton(
                    title = stringResource(R.string.admin_lift_ban),
                    subtitle = stringResource(R.string.admin_lift_ban_subtitle),
                    onClick = onLiftBan,
                )
            }
            if (isFrozen) {
                ActionButton(
                    title = stringResource(R.string.admin_lift_freeze),
                    subtitle = stringResource(R.string.admin_lift_freeze_subtitle),
                    onClick = onLiftFreeze,
                )
            }
            if (showDismissReport) {
                ActionButton(
                    title = stringResource(R.string.admin_reports_action_dismiss),
                    subtitle = stringResource(R.string.admin_reports_action_dismiss_subtitle),
                    onClick = onDismissReport,
                )
            }
            OutlinedTextField(
                value = warningText,
                onValueChange = { warningText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ActionButton(
                title = stringResource(R.string.admin_reports_action_warn),
                subtitle = stringResource(R.string.admin_reports_action_warn_subtitle),
                onClick = { onWarn(warningText) },
            )
            Text(
                stringResource(R.string.admin_reports_freeze_title),
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onFreeze(ModerationFreezeDuration.HOURS_24) }) {
                    Text(stringResource(R.string.admin_reports_freeze_24h), color = TabSelected)
                }
                TextButton(onClick = { onFreeze(ModerationFreezeDuration.HOURS_48) }) {
                    Text(stringResource(R.string.admin_reports_freeze_48h), color = TabSelected)
                }
                TextButton(onClick = { onFreeze(ModerationFreezeDuration.HOURS_72) }) {
                    Text(stringResource(R.string.admin_reports_freeze_72h), color = TabSelected)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (confirmBan) {
                Text(
                    stringResource(R.string.admin_reports_ban_confirm_message, title),
                    color = LikeRed,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBan,
                    colors = ButtonDefaults.buttonColors(containerColor = LikeRed),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.admin_reports_action_ban))
                }
            } else {
                ActionButton(
                    title = stringResource(R.string.admin_reports_action_ban),
                    subtitle = stringResource(R.string.admin_reports_action_ban_subtitle),
                    destructive = true,
                    onClick = { confirmBan = true },
                )
            }
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.common_cancel), color = colors.secondaryText)
            }
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    subtitle: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            color = if (destructive) LikeRed else colors.text,
        )
        Text(subtitle, color = colors.secondaryText, fontSize = 13.sp)
    }
}
