package com.example.spectroom.ui.screens.messages

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewReadySheet(
    uri: Uri,
    isSending: Boolean,
    onSend: () -> Unit,
    onDiscard: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    ModalBottomSheet(
        onDismissRequest = onDiscard,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.secondaryText.copy(alpha = 0.15f)),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSend,
                enabled = !isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
            ) {
                Text(stringResource(R.string.messages_send_message), fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDiscard, enabled = !isSending) {
                Text(stringResource(R.string.common_cancel), color = colors.secondaryText)
            }
        }
    }
}
