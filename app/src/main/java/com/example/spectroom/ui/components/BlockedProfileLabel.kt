package com.example.spectroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R

/** Mirrors iOS `BlockedProfileLabel` — red stamp over faded blocked rows. */
@Composable
fun BlockedProfileLabel(
    dimmed: Boolean = true,
    slanted: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val labelColor = if (dimmed) Color.Red.copy(alpha = 0.7f) else Color.Red
    Column(
        modifier = modifier.then(if (slanted) Modifier.rotate(-12f) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(1.5.dp)
                .background(labelColor),
        )
        Text(
            text = stringResource(R.string.messages_conversation_blocked),
            color = labelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            fontStyle = if (slanted) FontStyle.Italic else FontStyle.Normal,
        )
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(1.5.dp)
                .background(labelColor),
        )
    }
}
