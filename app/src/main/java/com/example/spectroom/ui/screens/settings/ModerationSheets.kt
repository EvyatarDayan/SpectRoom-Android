package com.example.spectroom.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.ui.theme.LocalSpectRoomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserSheet(
    userName: String,
    categories: List<String>,
    onSubmit: (category: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("Report $userName", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.text)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Why are you reporting this profile?", color = colors.secondaryText)
            Spacer(modifier = Modifier.height(12.dp))
            categories.forEach { category ->
                Text(
                    text = category,
                    color = colors.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSubmit(category) }
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}
