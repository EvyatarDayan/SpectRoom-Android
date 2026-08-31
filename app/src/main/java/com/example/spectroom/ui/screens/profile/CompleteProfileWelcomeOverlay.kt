package com.example.spectroom.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spectroom.R
import com.example.spectroom.ui.components.ConfettiBurst
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun CompleteProfileWelcomeOverlay(onContinue: () -> Unit) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .zIndex(20f),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiBurst(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .background(colors.card, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.profile_complete_welcome_title),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.profile_complete_welcome_message),
                color = colors.secondaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 18.dp),
            )
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
            ) {
                Text(stringResource(R.string.profile_complete_welcome_continue))
            }
        }
    }
}
