package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.data.model.AppLanguage
import com.example.spectroom.ui.components.TabBarMainContentInset
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

private val SectionCorner = 12.dp
private val SettingsTitleTopPadding = 12.dp
private val SettingsTitleBottomPadding = 20.dp
private val SectionTitleBottomPadding = 8.dp
private val RowVerticalPadding = 14.dp
private val IconSize = 24.dp

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    languageCode: String,
    notificationsEnabled: Boolean,
    blockedUsersCount: Int = 0,
    reportedProfilesCount: Int = 0,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onBlockedUsers: () -> Unit = {},
    onReportedProfiles: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onHelp: () -> Unit = {},
    onAbout: () -> Unit = {},
    onExportMessages: () -> Unit = {},
    isAdmin: Boolean = false,
    onAdmin: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val language = AppLanguage.fromCode(languageCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = TabBarMainContentInset),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SettingsTitleTopPadding, bottom = SettingsTitleBottomPadding),
        )

        SettingsSection(title = stringResource(R.string.settings_section_preferences)) {
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.settings_notifications),
                    subtitle = stringResource(R.string.settings_notifications_description),
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChange,
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Filled.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    subtitle = stringResource(R.string.settings_dark_mode_description),
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange,
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_description),
                    onClick = onLanguageClick,
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(text = language.flag, fontSize = 18.sp)
                            Text(
                                text = language.name,
                                color = colors.secondaryText,
                                fontSize = 14.sp,
                            )
                            SettingsChevron()
                        }
                    },
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_privacy_safety)) {
            SettingsCard {
                SettingsNavigationRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.settings_privacy_settings),
                    subtitle = stringResource(R.string.settings_privacy_safety_description),
                    onClick = onPrivacy,
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Filled.Block,
                    title = stringResource(
                        R.string.settings_blocked_users_count,
                        blockedUsersCount,
                    ),
                    subtitle = stringResource(R.string.settings_blocked_subtitle),
                    onClick = onBlockedUsers,
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Filled.Report,
                    title = stringResource(
                        R.string.settings_reported_profiles_count,
                        reportedProfilesCount,
                    ),
                    subtitle = stringResource(R.string.settings_reported_profiles_subtitle),
                    onClick = onReportedProfiles,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_account_data)) {
            SettingsCard {
                SettingsNavigationRow(
                    icon = Icons.Filled.Upload,
                    title = stringResource(R.string.settings_export_messages),
                    subtitle = stringResource(R.string.settings_export_messages_description),
                    onClick = onExportMessages,
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.settings_sign_out),
                    subtitle = stringResource(R.string.settings_sign_out_subtitle),
                    onClick = onSignOut,
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Filled.Delete,
                    title = stringResource(R.string.settings_delete_account),
                    subtitle = stringResource(R.string.settings_delete_account_subtitle),
                    onClick = onDeleteAccount,
                    titleColor = LikeRed,
                    showChevron = false,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_support_about)) {
            SettingsCard {
                SettingsNavigationRow(
                    icon = Icons.Filled.Help,
                    title = stringResource(R.string.settings_help),
                    subtitle = stringResource(R.string.settings_help_description),
                    onClick = onHelp,
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_description),
                    onClick = onAbout,
                )
            }
        }

        if (isAdmin) {
            SettingsSection(title = stringResource(R.string.admin_section_title)) {
                SettingsCard {
                    SettingsNavigationRow(
                        icon = Icons.Filled.Lock,
                        title = stringResource(R.string.admin_console_title),
                        subtitle = stringResource(R.string.admin_console_subtitle),
                        onClick = onAdmin,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.secondaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = SectionTitleBottomPadding),
        )
        content()
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SectionCorner))
            .background(colors.card),
        content = content,
    )
}

@Composable
private fun SettingsDivider() {
    val colors = LocalSpectRoomColors.current
    HorizontalDivider(
        color = colors.border,
        modifier = Modifier.padding(start = 16.dp + IconSize + 12.dp),
    )
}

@Composable
private fun SettingsChevron() {
    val colors = LocalSpectRoomColors.current
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = colors.secondaryText,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun SettingsLeadingIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TabSelected,
        modifier = Modifier.size(IconSize),
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsLeadingIcon(icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontSize = 12.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .padding(start = 8.dp)
                .scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.card,
                checkedTrackColor = TabSelected,
            ),
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color? = null,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsLeadingIcon(icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor ?: colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = colors.secondaryText,
                fontSize = 12.sp,
            )
        }
        when {
            trailing != null -> trailing()
            showChevron -> SettingsChevron()
        }
    }
}
