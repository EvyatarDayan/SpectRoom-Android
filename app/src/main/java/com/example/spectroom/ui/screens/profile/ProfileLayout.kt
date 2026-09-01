package com.example.spectroom.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

object ProfilePhotoStyle {
    val cardCorner = 24.dp
    val photoCorner = 18.dp
    val mainPhotoHeight = 280.dp
    val galleryPhotoSize = 104.dp
}

@Composable
fun ProfileScreenTopBar(
    title: String,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    applyStatusBarPadding: Boolean = false,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            leading?.invoke()
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = if (leading != null || trailing != null) 20.sp else 28.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 72.dp)
                .align(Alignment.Center),
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            trailing?.invoke()
        }
    }
}

@Composable
fun ProfilePillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    val colors = LocalSpectRoomColors.current
    val contentColor = when {
        !enabled -> colors.secondaryText
        emphasized -> TabSelected
        else -> colors.text
    }
    Text(
        text = text,
        color = contentColor,
        fontSize = 15.sp,
        fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .shadow(if (enabled) 2.dp else 0.dp, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .background(colors.card)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
fun ProfileCircleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(2.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .border(1.dp, colors.border, CircleShape)
            .background(colors.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun ProfileTextLink(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = TabSelected,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun ProfileMainPhotoCard(
    imageUrl: String?,
    onClick: (() -> Unit)? = null,
    showsCameraBadge: Boolean = false,
    placeholderText: String? = null,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(ProfilePhotoStyle.cardCorner), clip = false)
            .clip(RoundedCornerShape(ProfilePhotoStyle.cardCorner))
            .height(ProfilePhotoStyle.mainPhotoHeight)
            .background(colors.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(44.dp),
                    )
                    if (placeholderText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(placeholderText, color = colors.secondaryText, fontSize = 15.sp)
                    }
                }
            }
        }
        if (showsCameraBadge) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = TabSelected,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(34.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
fun ProfileGalleryPhotosCard(
    photoUrls: List<String>,
    title: String? = null,
    emptyMessage: String? = null,
    footerText: String? = null,
    onSelectPhoto: ((Int) -> Unit)? = null,
    onDeletePhoto: ((String) -> Unit)? = null,
    onAddPhoto: (() -> Unit)? = null,
    maxPhotos: Int = 5,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(ProfilePhotoStyle.cardCorner), clip = false)
            .clip(RoundedCornerShape(ProfilePhotoStyle.cardCorner))
            .background(colors.card)
            .padding(16.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        val visible = photoUrls.take(maxPhotos)
        if (visible.isEmpty() && onAddPhoto == null) {
            Text(
                text = emptyMessage.orEmpty(),
                color = colors.secondaryText,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
            )
        } else {
            val columns = 3
            val cells = buildList {
                visible.forEachIndexed { index, url ->
                    add(GridCell.Photo(url, index))
                }
                if (onAddPhoto != null && visible.size < maxPhotos) {
                    add(GridCell.Add)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cells.chunked(columns).forEach { rowCells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowCells.forEach { cell ->
                            when (cell) {
                                is GridCell.Photo -> {
                                    GalleryThumb(
                                        url = cell.url,
                                        onClick = onSelectPhoto?.let { { it(cell.index) } },
                                        onDelete = onDeletePhoto?.let { { it(cell.url) } },
                                    )
                                }
                                GridCell.Add -> {
                                    AddPhotoTile(
                                        onClick = onAddPhoto!!,
                                        enabled = visible.size < maxPhotos,
                                    )
                                }
                            }
                        }
                        repeat(columns - rowCells.size) {
                            Spacer(modifier = Modifier.size(ProfilePhotoStyle.galleryPhotoSize))
                        }
                    }
                }
            }
        }

        if (!footerText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(footerText, color = colors.secondaryText, fontSize = 12.sp)
        }
    }
}

private sealed interface GridCell {
    data class Photo(val url: String, val index: Int) : GridCell
    data object Add : GridCell
}

@Composable
private fun GalleryThumb(
    url: String,
    onClick: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .size(ProfilePhotoStyle.galleryPhotoSize)
            .shadow(3.dp, RoundedCornerShape(ProfilePhotoStyle.photoCorner), clip = false)
            .clip(RoundedCornerShape(ProfilePhotoStyle.photoCorner))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (onDelete != null) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.settings_cancel),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .clickable(onClick = onDelete)
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun AddPhotoTile(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .size(ProfilePhotoStyle.galleryPhotoSize)
            .clip(RoundedCornerShape(ProfilePhotoStyle.photoCorner))
            .background(colors.card)
            .border(
                width = 1.5.dp,
                color = TabSelected.copy(alpha = 0.25f),
                shape = RoundedCornerShape(ProfilePhotoStyle.photoCorner),
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = TabSelected, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_edit_add),
            color = TabSelected,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = TabSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun ProfileSubsectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        color = TabSelected,
    )
}

@Composable
fun ProfileBodyText(
    text: String,
    italic: Boolean = false,
    secondary: Boolean = false,
) {
    val colors = LocalSpectRoomColors.current
    Text(
        text = text,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = if (secondary) colors.secondaryText else colors.text,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )
}

@Composable
fun ProfileCompletionBar(percent: Int) {
    val colors = LocalSpectRoomColors.current
    val progressColor = if (percent == 100) Color(0xFF4CAF50) else TabSelected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card),
    ) {
        HorizontalDivider(color = colors.border)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (percent == 100) Icons.Filled.CheckCircle else Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = progressColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.profile_edit_completion, percent),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$percent%",
                    color = progressColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(progressColor.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = progressColor,
                trackColor = colors.secondaryText.copy(alpha = if (colors.isDark) 0.18f else 0.10f),
                strokeCap = StrokeCap.Round,
            )
        }
        HorizontalDivider(color = colors.border)
    }
}

@Composable
fun ProfilePreferenceDivider() {
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.Gray.copy(alpha = 0.3f)),
    )
    Spacer(modifier = Modifier.height(4.dp))
}
