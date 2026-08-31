package com.example.spectroom.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R

@Composable
fun PhotoShareWarningOverlay(
    onContinue: (dontShowAgain: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var understood by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onCancel)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp),
            ) {
                Text(
                    stringResource(R.string.photo_share_warning_subtitle),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    stringResource(R.string.photo_share_warning_bullets_intro),
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    R.string.photo_share_warning_bullet_intimate,
                    R.string.photo_share_warning_bullet_address,
                    R.string.photo_share_warning_bullet_workplace,
                ).forEach { res ->
                    Text(
                        "• ${stringResource(res)}",
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { understood = !understood }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (understood) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.photo_share_warning_dont_show_again),
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onContinue(understood) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(stringResource(R.string.photo_share_warning_continue), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
