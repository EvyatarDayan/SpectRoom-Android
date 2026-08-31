package com.example.spectroom.ui.screens.discover

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun BlockConfirmationOverlay(
    firstName: String,
    onDismiss: () -> Unit,
    onBlock: () -> Unit,
    onBlockAndReport: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var showingSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .zIndex(30f)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.background)
                            .padding(4.dp),
                    )
                }
            }
            if (showingSuccess) {
                Text(
                    text = stringResource(R.string.profile_block_success_title, firstName),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_done), color = TabSelected)
                }
            } else {
                Text(
                    text = stringResource(R.string.profile_block_confirm_title, firstName),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.profile_block_confirm_message),
                    color = colors.text,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onBlockAndReport,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_block_confirm_block_and_report),
                            color = LikeRed,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onBlock()
                            showingSuccess = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_block_confirm_action),
                            color = LikeRed,
                        )
                    }
                }
            }
        }
    }
}
