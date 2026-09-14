package com.example.spectroom.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

private enum class QuickMessageCategory {
    LOW_PRESSURE,
    CASUAL,
    QUESTIONS,
}

/** Matches iOS NavigationView sheet presentation — tall fixed height, content scrolls inside. */
private const val QuickMessageSheetHeightFraction = 0.92f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreMessageOptionsSheet(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var category by remember { mutableStateOf(QuickMessageCategory.LOW_PRESSURE) }
    val messages = when (category) {
        QuickMessageCategory.LOW_PRESSURE -> stringArrayResource(R.array.quick_msg_low)
        QuickMessageCategory.CASUAL -> stringArrayResource(R.array.quick_msg_casual)
        QuickMessageCategory.QUESTIONS -> stringArrayResource(R.array.quick_msg_question)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(QuickMessageSheetHeightFraction)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.matches_pre_message_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.matches_pre_message_subtitle),
                        color = colors.secondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.common_close),
                    color = colors.text,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, colors.border, RoundedCornerShape(50))
                        .background(colors.card)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip(
                    label = stringResource(R.string.quick_msg_category_low_pressure),
                    selected = category == QuickMessageCategory.LOW_PRESSURE,
                    onClick = { category = QuickMessageCategory.LOW_PRESSURE },
                )
                CategoryChip(
                    label = stringResource(R.string.quick_msg_category_casual),
                    selected = category == QuickMessageCategory.CASUAL,
                    onClick = { category = QuickMessageCategory.CASUAL },
                )
                CategoryChip(
                    label = stringResource(R.string.quick_msg_category_questions),
                    selected = category == QuickMessageCategory.QUESTIONS,
                    onClick = { category = QuickMessageCategory.QUESTIONS },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages.toList()) { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.card)
                            .clickable { onSelect(message) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        Text("+", color = colors.secondaryText, fontWeight = FontWeight.SemiBold)
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    Text(
        text = label,
        color = if (selected) Color.White else colors.text,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) TabSelected else colors.card)
            .then(
                if (selected) Modifier else Modifier.border(1.dp, colors.border, RoundedCornerShape(20.dp)),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
