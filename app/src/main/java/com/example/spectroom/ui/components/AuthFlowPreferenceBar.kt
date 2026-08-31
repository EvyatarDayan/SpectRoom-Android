package com.example.spectroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.data.model.AppLanguage
import com.example.spectroom.ui.theme.LocalSpectRoomColors

@Composable
fun AuthFlowPreferenceBar(
    isDarkMode: Boolean,
    languageCode: String,
    onToggleDarkMode: () -> Unit,
    onLanguageClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val language = AppLanguage.fromCode(languageCode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card.copy(alpha = 0.92f))
                .clickable(onClick = onToggleDarkMode)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = null,
                tint = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFFFFC107) else androidx.compose.ui.graphics.Color(0xFFFF9800),
            )
            Text(
                text = stringResource(
                    if (isDarkMode) R.string.settings_light_mode else R.string.settings_dark_mode,
                ),
                color = colors.text,
                fontSize = 13.sp,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card.copy(alpha = 0.92f))
                .clickable(onClick = onLanguageClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(language.flag, fontSize = 16.sp)
            Text(language.name, color = colors.text, fontSize = 13.sp)
        }
    }
}
