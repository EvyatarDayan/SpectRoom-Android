package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.data.model.AppLanguage
import com.example.spectroom.data.model.PlanTier
import com.example.spectroom.ui.components.TabBarBodyHeight
import com.example.spectroom.ui.theme.LikeGreen
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

private val SectionCorner = 12.dp
private val SettingsTitleTopPadding = 12.dp
/** Extra gap between the last settings row and the top of the tab bar. */
private val SettingsMenuClearance = 20.dp
private val SettingsTitleLineHeight = 40.dp
private val SettingsTitleBottomPadding = 20.dp
private val SettingsTitleBarReserve =
    SettingsTitleTopPadding + SettingsTitleLineHeight + SettingsTitleBottomPadding
private val SectionTitleBottomPadding = 8.dp
private val RowVerticalPadding = 14.dp
private val IconSize = 24.dp
/** Mirror iOS danger-zone coral `Color(red: 1.0, green: 0.45, blue: 0.45)`. */
private val DangerZoneCoral = Color(red = 1f, green = 0.45f, blue = 0.45f)

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    languageCode: String,
    notificationsEnabled: Boolean,
    blockedUsersCount: Int = 0,
    reportedProfilesCount: Int = 0,
    accountEmail: String = "",
    planTier: PlanTier = PlanTier.FREE,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onBlockedUsers: () -> Unit = {},
    onReportedProfiles: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onHelp: () -> Unit = {},
    onAbout: () -> Unit = {},
    onExportMessages: () -> Unit = {},
    onAccountBilling: () -> Unit = {},
    isAdmin: Boolean = false,
    onAdmin: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onResetUserData: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val language = AppLanguage.fromCode(languageCode)
    val isDebugBuild = (LocalContext.current.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Clear the white tab bar + system nav + a visible gap above the menu.
    val settingsBottomPadding = navBarBottom + TabBarBodyHeight + SettingsMenuClearance
    var showingSignOutConfirmation by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(SettingsTitleBarReserve))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = settingsBottomPadding),
            ) {
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
                    icon = Icons.Filled.Upload,
                    title = stringResource(R.string.settings_export_messages),
                    subtitle = stringResource(R.string.settings_export_messages_description),
                    onClick = onExportMessages,
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

        SettingsSection(title = stringResource(R.string.settings_section_account)) {
            SettingsCard {
                SettingsNavigationRow(
                    icon = Icons.Filled.CreditCard,
                    title = stringResource(R.string.settings_account_billing),
                    subtitle = stringResource(R.string.settings_account_billing_description),
                    onClick = onAccountBilling,
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(text = planTier.titleStars, fontSize = 16.sp)
                            SettingsChevron()
                        }
                    },
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.settings_sign_out),
                    // Mirror iOS: show the signed-in email under Sign Out.
                    subtitle = accountEmail,
                    onClick = { showingSignOutConfirmation = true },
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

        SettingsSection(title = stringResource(R.string.settings_section_danger_zone)) {
            SettingsCard {
                if (isDebugBuild) {
                    SettingsNavigationRow(
                        icon = Icons.Filled.RestartAlt,
                        title = stringResource(R.string.settings_reset_user_data),
                        subtitle = stringResource(R.string.settings_reset_user_data_description),
                        onClick = onResetUserData,
                        iconTint = DangerZoneCoral,
                        titleColor = DangerZoneCoral,
                        subtitleColor = DangerZoneCoral,
                        showChevron = false,
                    )
                    SettingsDivider()
                }
                SettingsNavigationRow(
                    icon = Icons.Filled.Delete,
                    title = stringResource(R.string.settings_delete_account),
                    subtitle = stringResource(R.string.settings_delete_account_subtitle),
                    onClick = onDeleteAccount,
                    iconTint = DangerZoneCoral,
                    titleColor = DangerZoneCoral,
                    subtitleColor = DangerZoneCoral,
                    showChevron = false,
                )
            }
        }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    top = SettingsTitleTopPadding,
                    bottom = SettingsTitleBottomPadding,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
        }

        if (showingSignOutConfirmation) {
            SignOutConfirmationOverlay(
                onCancel = { showingSignOutConfirmation = false },
                onConfirm = {
                    showingSignOutConfirmation = false
                    onSignOut()
                },
            )
        }
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
private fun SettingsLeadingIcon(
    icon: ImageVector,
    tint: Color = TabSelected,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
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
    iconTint: Color = TabSelected,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
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
        SettingsLeadingIcon(icon, tint = iconTint)
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
                color = subtitleColor ?: colors.secondaryText,
                fontSize = 12.sp,
            )
        }
        when {
            trailing != null -> trailing()
            showChevron -> SettingsChevron()
        }
    }
}

/** Matches unblock / auth confirm chrome — title + Cancel | Yes. */
@Composable
private fun SignOutConfirmationOverlay(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .zIndex(40f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.settings_sign_out_confirm_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 18.dp),
            )
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
                        text = stringResource(R.string.settings_sign_out_confirm_yes),
                        color = LikeGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
