package com.example.spectroom.ui.screens.profile

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R
import com.example.spectroom.data.remote.AddressSuggestion
import com.example.spectroom.domain.SwedenLocation
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import kotlinx.coroutines.delay

@Composable
fun AddressSearchSheet(
    isSearching: Boolean,
    suggestions: List<AddressSuggestion>,
    onQueryChange: (String) -> Unit,
    onSelect: (AddressSuggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var query by remember { mutableStateOf("") }
    var selectedSuggestion by remember { mutableStateOf<AddressSuggestion?>(null) }
    var showOutsideSwedenAlert by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(query) {
        delay(350)
        onQueryChange(query)
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                ProfileScreenTopBar(
                    title = stringResource(R.string.address_search_title),
                    applyStatusBarPadding = true,
                    leading = {
                        ProfilePillButton(
                            text = stringResource(R.string.common_cancel),
                            onClick = onDismiss,
                        )
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(20.dp),
                        )
                        BasicTextField(
                            value = query,
                            onValueChange = { value ->
                                query = value
                                if (value.isBlank()) {
                                    selectedSuggestion = null
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = colors.text,
                                fontSize = 16.sp,
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(TabSelected),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.address_search_placeholder),
                                            color = colors.secondaryText,
                                            fontSize = 16.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    selectedSuggestion = null
                                    onQueryChange("")
                                },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null,
                                    tint = colors.secondaryText,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.address_search_sweden_only_notice),
                        color = colors.secondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (query.isNotBlank()) {
                        when {
                            isSearching && suggestions.isEmpty() -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(
                                        color = TabSelected,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    Text(
                                        text = stringResource(R.string.address_search_searching),
                                        color = colors.secondaryText,
                                        fontSize = 15.sp,
                                    )
                                }
                            }
                            suggestions.isEmpty() -> {
                                Text(
                                    text = stringResource(R.string.address_search_no_results_sweden),
                                    color = colors.secondaryText,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                )
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(suggestions, key = { it.displayString }) { suggestion ->
                                        AddressSearchResultRow(
                                            suggestion = suggestion,
                                            isSelected = selectedSuggestion?.displayString == suggestion.displayString,
                                            onClick = { selectedSuggestion = suggestion },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        AddressMapPreview(
                            latitude = selectedSuggestion?.latitude,
                            longitude = selectedSuggestion?.longitude,
                            modifier = Modifier.fillMaxSize(),
                        )
                        selectedSuggestion?.let { suggestion ->
                            Text(
                                text = suggestion.displayString,
                                color = colors.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.card.copy(alpha = 0.94f))
                                    .padding(12.dp),
                            )
                        }
                    }

                    if (selectedSuggestion != null) {
                        Button(
                            onClick = {
                                val suggestion = selectedSuggestion ?: return@Button
                                if (!SwedenLocation.contains(suggestion.latitude, suggestion.longitude)) {
                                    showOutsideSwedenAlert = true
                                    return@Button
                                }
                                onSelect(suggestion)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.address_search_use_address),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showOutsideSwedenAlert) {
        AlertDialog(
            onDismissRequest = { showOutsideSwedenAlert = false },
            title = { Text(stringResource(R.string.address_search_title)) },
            text = { Text(stringResource(R.string.address_search_outside_sweden)) },
            confirmButton = {
                TextButton(onClick = { showOutsideSwedenAlert = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

@Composable
private fun AddressSearchResultRow(
    suggestion: AddressSuggestion,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) TabSelected.copy(alpha = 0.12f) else colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = if (isSelected) TabSelected else colors.secondaryText,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = suggestion.title,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            if (suggestion.subtitle.isNotBlank()) {
                Text(
                    text = suggestion.subtitle,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = TabSelected,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
