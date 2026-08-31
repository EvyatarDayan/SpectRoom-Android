package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R
import com.example.spectroom.data.model.AppLanguage
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun LanguagePickerSheet(
    selectedCode: String,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.language_selection_title),
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                items(AppLanguage.all, key = { it.code }) { language ->
                    LanguageRow(
                        language = language,
                        isSelected = language.code == selectedCode,
                        onClick = { onSelect(language) },
                    )
                    HorizontalDivider(color = colors.border)
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = language.flag,
            fontSize = 22.sp,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = language.name,
            modifier = Modifier.weight(1f),
            color = colors.text,
            fontSize = 16.sp,
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = TabSelected,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
