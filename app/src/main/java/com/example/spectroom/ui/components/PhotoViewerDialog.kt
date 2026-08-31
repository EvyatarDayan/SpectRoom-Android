package com.example.spectroom.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@Composable
fun PhotoViewerDialog(
    photos: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit = {},
) {
    if (photos.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size },
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                AsyncImage(
                    model = photos[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            TextButton(
                onClick = { onSave(photos[pagerState.currentPage]) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        com.example.spectroom.R.string.messages_save_photo,
                    ),
                    color = Color.White,
                )
            }
            if (photos.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${pagerState.currentPage + 1}/${photos.size}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                    photos.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                .background(
                                    if (index == pagerState.currentPage) {
                                        Color.White
                                    } else {
                                        Color.White.copy(alpha = 0.4f)
                                    },
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}
