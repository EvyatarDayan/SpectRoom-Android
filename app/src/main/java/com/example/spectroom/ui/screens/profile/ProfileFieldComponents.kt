package com.example.spectroom.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.data.model.SensoryPreferenceOption
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.PassRed
import com.example.spectroom.ui.theme.TabSelected

object ProfileFieldStyle {
    val fieldCorner = 14.dp
    val sectionCorner = 12.dp
    val fieldBorderWidth = 1.5.dp
    val sectionShadow = 2.dp
}

fun Modifier.profileValidationBorder(
    isValid: Boolean,
    hasAttemptedSave: Boolean,
    cornerRadius: androidx.compose.ui.unit.Dp = ProfileFieldStyle.sectionCorner,
): Modifier {
    if (!hasAttemptedSave || isValid) return this
    return border(
        width = ProfileFieldStyle.fieldBorderWidth,
        color = PassRed,
        shape = RoundedCornerShape(cornerRadius),
    )
}

@Composable
private fun profileFieldBackground(): Color = TabSelected.copy(alpha = 0.08f)

@Composable
private fun profileFieldBorder(): Color = TabSelected.copy(alpha = 0.28f)

@Composable
fun ProfileEditSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(ProfileFieldStyle.sectionShadow, RoundedCornerShape(ProfileFieldStyle.sectionCorner), clip = false)
            .clip(RoundedCornerShape(ProfileFieldStyle.sectionCorner))
            .background(colors.card)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun ProfileFieldHeading(title: String) {
    val colors = LocalSpectRoomColors.current
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        color = colors.text,
    )
}

@Composable
fun ProfileFieldSubheading(title: String) {
    val colors = LocalSpectRoomColors.current
    Text(
        text = title,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = colors.text,
    )
}

@Composable
fun ProfileFieldCaption(text: String) {
    val colors = LocalSpectRoomColors.current
    Text(
        text = text,
        fontSize = 12.sp,
        color = colors.secondaryText,
    )
}

@Composable
fun ProfileSectionDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.Gray.copy(alpha = 0.3f)),
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun ProfileBlueField(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .background(profileFieldBackground())
            .border(
                ProfileFieldStyle.fieldBorderWidth,
                profileFieldBorder(),
                RoundedCornerShape(ProfileFieldStyle.fieldCorner),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
fun ProfileBlueFieldText(
    text: String,
    italic: Boolean = false,
    secondary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    ProfileBlueField(modifier = modifier) {
        Text(
            text = text,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = if (secondary) colors.secondaryText else colors.text,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        )
    }
}

@Composable
fun ProfileReadOnlyField(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.secondaryText,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .background(profileFieldBackground())
            .border(ProfileFieldStyle.fieldBorderWidth, profileFieldBorder(), RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    )
}

@Composable
fun ProfileBioField(
    value: String,
    onValueChange: (String) -> Unit,
    characterCounter: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 500) onValueChange(it) },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                color = colors.text,
            ),
            cursorBrush = SolidColor(TabSelected),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(ProfileFieldStyle.fieldCorner))
                .background(profileFieldBackground())
                .border(ProfileFieldStyle.fieldBorderWidth, profileFieldBorder(), RoundedCornerShape(ProfileFieldStyle.fieldCorner))
                .padding(16.dp),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = characterCounter,
                fontSize = 12.sp,
                color = if (value.length >= 500) Color(0xFFE53935) else colors.secondaryText,
            )
        }
    }
}

@Composable
fun ProfileDateOfBirthField(
    dateText: String,
    ageText: String,
    ageIsValid: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .background(profileFieldBackground())
            .border(ProfileFieldStyle.fieldBorderWidth, profileFieldBorder(), RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = dateText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ageText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (ageIsValid) TabSelected else Color(0xFFE53935),
                textAlign = TextAlign.Center,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(20.dp),
        )
    }
}

@Composable
fun ProfileLocationField(
    locationText: String,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .background(profileFieldBackground())
            .border(ProfileFieldStyle.fieldBorderWidth, profileFieldBorder(), RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = locationText,
            fontSize = 16.sp,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = TabSelected,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun ProfilePreferredAgeBadge(rangeText: String) {
    Text(
        text = rangeText,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = TabSelected,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TabSelected.copy(alpha = 0.12f))
            .border(ProfileFieldStyle.fieldBorderWidth, TabSelected.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun ProfileSelectionChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .background(if (isSelected) TabSelected.copy(alpha = 0.12f) else colors.card)
            .border(
                width = if (isSelected) ProfileFieldStyle.fieldBorderWidth else 1.dp,
                color = if (isSelected) TabSelected.copy(alpha = 0.45f) else colors.border,
                shape = RoundedCornerShape(ProfileFieldStyle.fieldCorner),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) TabSelected else colors.secondaryText.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) TabSelected else colors.text,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
fun ProfileChipGrid(
    items: List<Pair<String, String>>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    columns: Int = 2,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { (value, label) ->
                    ProfileSelectionChip(
                        title = label,
                        isSelected = selected.any { it.equals(value, ignoreCase = true) },
                        onClick = { onToggle(value) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ProfileChipList(
    items: List<Pair<String, String>>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (value, label) ->
            ProfileSelectionChip(
                title = label,
                isSelected = selected.any { it.equals(value, ignoreCase = true) },
                onClick = { onToggle(value) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ProfileSensoryPreferenceList(
    selected: List<String>,
    onToggle: (String) -> Unit,
    onInfo: (SensoryPreferenceOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SensoryPreferenceOption.entries.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileSelectionChip(
                    title = stringResource(option.titleRes),
                    isSelected = selected.any { it.equals(option.rawValue, ignoreCase = true) },
                    onClick = { onToggle(option.rawValue) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_sensory_info),
                    contentDescription = stringResource(option.subtitleRes),
                    tint = TabSelected,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onInfo(option) }
                        .padding(5.dp),
                )
            }
        }
    }
}

@Composable
fun ProfileChipListSingle(
    items: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (value, label) ->
            ProfileSelectionChip(
                title = label,
                isSelected = selected.equals(value, ignoreCase = true),
                onClick = { onSelect(value) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
