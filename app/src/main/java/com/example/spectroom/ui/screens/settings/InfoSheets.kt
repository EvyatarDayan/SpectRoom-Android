package com.example.spectroom.ui.screens.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors

private enum class DeleteAccountStep {
    SURE,
    PASSWORD,
    SUCCESS,
}

@Composable
fun DeleteAccountSheet(
    isLoading: Boolean,
    errorMessage: String?,
    succeeded: Boolean,
    onConfirm: (password: String) -> Unit,
    onSuccessAcknowledged: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var step by remember { mutableStateOf(DeleteAccountStep.SURE) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(succeeded) {
        if (succeeded) step = DeleteAccountStep.SUCCESS
    }

    Dialog(
        onDismissRequest = {
            if (step != DeleteAccountStep.SUCCESS && !isLoading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(enabled = step != DeleteAccountStep.SUCCESS && !isLoading) {
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .clickable(enabled = false) {}
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (step == DeleteAccountStep.SUCCESS) {
                        stringResource(R.string.settings_delete_account_success)
                    } else {
                        stringResource(R.string.settings_delete_account_confirm_title)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = if (step == DeleteAccountStep.SUCCESS) 20.sp else 18.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                when (step) {
                    DeleteAccountStep.SURE -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.settings_delete_account_confirm_message),
                            color = colors.secondaryText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    DeleteAccountStep.PASSWORD -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.settings_delete_account_password_message),
                            color = colors.secondaryText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = {
                                Text(stringResource(R.string.settings_delete_account_password_placeholder))
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(8.dp),
                        )
                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                errorMessage,
                                color = LikeRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                        if (isLoading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            CircularProgressIndicator(color = LikeRed)
                        }
                    }
                    DeleteAccountStep.SUCCESS -> Unit
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.border)
                when (step) {
                    DeleteAccountStep.SURE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_cancel), color = colors.text)
                            }
                            VerticalDivider(modifier = Modifier.height(44.dp), color = colors.border)
                            TextButton(
                                onClick = {
                                    step = DeleteAccountStep.PASSWORD
                                    password = ""
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    stringResource(R.string.settings_delete_account_yes),
                                    color = LikeRed,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    DeleteAccountStep.PASSWORD -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.settings_cancel), color = colors.text)
                            }
                            VerticalDivider(modifier = Modifier.height(44.dp), color = colors.border)
                            TextButton(
                                onClick = { onConfirm(password) },
                                enabled = password.isNotBlank() && !isLoading,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    stringResource(R.string.common_ok),
                                    color = if (password.isNotBlank() && !isLoading) {
                                        LikeRed
                                    } else {
                                        colors.secondaryText
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    DeleteAccountStep.SUCCESS -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.width(1.dp).weight(1f))
                            VerticalDivider(modifier = Modifier.height(44.dp), color = colors.border)
                            TextButton(
                                onClick = onSuccessAcknowledged,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    stringResource(R.string.common_ok),
                                    color = LikeRed,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
