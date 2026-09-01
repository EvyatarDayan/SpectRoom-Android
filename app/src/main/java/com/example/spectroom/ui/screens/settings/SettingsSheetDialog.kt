package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.spectroom.ui.theme.LocalSpectRoomColors

internal val SettingsSheetTopCornerRadius = 20.dp

@Composable
internal fun SettingsFullScreenSheet(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
internal fun Modifier.settingsSheetSurface(): Modifier {
    val colors = LocalSpectRoomColors.current
    val sheetShape = RoundedCornerShape(
        topStart = SettingsSheetTopCornerRadius,
        topEnd = SettingsSheetTopCornerRadius,
    )
    return fillMaxSize()
        .clip(sheetShape)
        .background(colors.background, sheetShape)
        .statusBarsPadding()
}

@Composable
internal fun SettingsSheetColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.settingsSheetSurface(),
        content = content,
    )
}
