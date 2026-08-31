package com.example.spectroom.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.data.model.ProfileCard
import com.example.spectroom.ui.components.ConfettiBurst
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchModal(
    profile: ProfileCard,
    currentUserPhotoUrl: String?,
    onSendMessage: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onMaybeLater,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ConfettiBurst(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .align(Alignment.TopCenter),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.match_its_a_match),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = LikeRed,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = currentUserPhotoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(-8f)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .background(colors.secondaryText.copy(alpha = 0.2f)),
                    )
                    AsyncImage(
                        model = profile.imageURL,
                        contentDescription = profile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(x = (-20).dp)
                            .size(100.dp)
                            .rotate(8f)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .background(colors.secondaryText.copy(alpha = 0.2f)),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.match_liked_each_other, profile.name),
                    color = colors.text,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSendMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(stringResource(R.string.match_send_message))
                }
                TextButton(onClick = onMaybeLater) {
                    Text(stringResource(R.string.match_maybe_later), color = colors.secondaryText)
                }
            }
        }
    }
}
