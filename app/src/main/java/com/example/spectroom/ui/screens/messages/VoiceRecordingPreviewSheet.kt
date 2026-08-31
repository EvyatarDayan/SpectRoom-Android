package com.example.spectroom.ui.screens.messages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordingPreviewSheet(
    durationSeconds: Double,
    localPath: String?,
    isSending: Boolean,
    onSend: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    ModalBottomSheet(
        onDismissRequest = { if (!isSending) onDiscard() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.messages_voice_ready),
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(12.dp))
            VoiceMessagePlayer(
                audioUrl = null,
                localFile = localPath?.let { File(it) }?.takeIf { it.exists() },
                displayDurationSeconds = durationSeconds,
                isOutgoing = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = !isSending,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.common_delete), color = LikeRed)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onSend,
                    enabled = !isSending,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(
                        if (isSending) {
                            stringResource(R.string.messages_sending)
                        } else {
                            stringResource(R.string.messages_send)
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
