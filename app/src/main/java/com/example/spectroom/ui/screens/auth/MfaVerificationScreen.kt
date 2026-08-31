package com.example.spectroom.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.data.model.PhoneCountry
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import com.example.spectroom.viewmodel.MfaUiMode
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaVerificationScreen(
    mode: MfaUiMode,
    maskedPhone: String,
    emailAddress: String,
    emailVerified: Boolean,
    resolvedPhoneE164: String,
    smsSent: Boolean,
    smsFailed: Boolean,
    smsRateLimited: Boolean,
    editingPhone: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onSendVerificationEmail: () -> Unit,
    onCheckEmailVerified: () -> Unit,
    onResolvePhone: () -> Unit,
    onSaveMissingPhone: (phoneE164: String) -> Unit,
    onSaveEditedPhone: (phoneE164: String) -> Unit,
    onSetEditingPhone: (Boolean) -> Unit,
    onSendCode: (phoneE164: String?) -> Unit,
    onConfirm: (code: String) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var country by remember { mutableStateOf(PhoneCountry.Sweden) }
    var nationalNumber by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val isEnroll = mode == MfaUiMode.ENROLL
    val waitingForEmail = isEnroll && !emailVerified
    val e164 = PhoneCountry.buildE164(country, nationalNumber)
    val phoneMissing = isEnroll && emailVerified && resolvedPhoneE164.isBlank() && !editingPhone
    val showPhoneEditor = isEnroll && emailVerified && (phoneMissing || editingPhone)
    val showOtp = !waitingForEmail && !showPhoneEditor && (smsSent || !isEnroll)

    LaunchedEffect(Unit) {
        if (isEnroll) onResolvePhone()
    }

    LaunchedEffect(waitingForEmail) {
        if (!waitingForEmail) return@LaunchedEffect
        while (true) {
            delay(4_000)
            onCheckEmailVerified()
        }
    }

    LaunchedEffect(emailVerified, resolvedPhoneE164, smsSent, smsFailed, editingPhone, isLoading) {
        if (!isEnroll || waitingForEmail || editingPhone || phoneMissing) return@LaunchedEffect
        if (resolvedPhoneE164.isBlank() || smsSent || smsFailed || isLoading) return@LaunchedEffect
        onSendCode(resolvedPhoneE164)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when {
                waitingForEmail -> stringResource(R.string.mfa_email_title)
                phoneMissing -> stringResource(R.string.mfa_phone_missing_title)
                editingPhone -> stringResource(R.string.mfa_edit_number_title)
                smsFailed && smsRateLimited -> stringResource(R.string.mfa_rate_limited_title)
                isEnroll -> stringResource(R.string.mfa_enroll_title)
                else -> stringResource(R.string.mfa_signin_title)
            },
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                waitingForEmail -> stringResource(
                    R.string.mfa_email_subtitle,
                    emailAddress.ifBlank { stringResource(R.string.mfa_email_fallback) },
                )
                phoneMissing -> stringResource(R.string.mfa_phone_missing_subtitle)
                editingPhone -> stringResource(R.string.mfa_edit_number_subtitle)
                smsFailed && smsRateLimited -> stringResource(R.string.mfa_rate_limited_subtitle)
                isEnroll && smsSent -> stringResource(
                    R.string.mfa_code_sent_to,
                    resolvedPhoneE164.ifBlank { maskedPhone },
                )
                isEnroll -> stringResource(R.string.mfa_enroll_subtitle)
                else -> stringResource(R.string.mfa_signin_subtitle, maskedPhone)
            },
            color = colors.secondaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            waitingForEmail -> {
                Button(
                    onClick = onSendVerificationEmail,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(stringResource(R.string.mfa_email_send), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCheckEmailVerified,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.card),
                ) {
                    Text(stringResource(R.string.mfa_email_check), color = colors.text)
                }
            }

            showPhoneEditor -> {
                PhoneEditorRow(
                    country = country,
                    nationalNumber = nationalNumber,
                    onNationalChange = { nationalNumber = it.filter(Char::isDigit).take(15) },
                    onPickCountry = { showCountryPicker = true },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val phone = e164 ?: return@Button
                        if (editingPhone) onSaveEditedPhone(phone) else onSaveMissingPhone(phone)
                    },
                    enabled = !isLoading && e164 != null,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(
                        if (editingPhone) {
                            stringResource(R.string.mfa_save_and_resend)
                        } else {
                            stringResource(R.string.mfa_save_phone)
                        },
                    )
                }
                if (editingPhone) {
                    TextButton(onClick = { onSetEditingPhone(false) }, enabled = !isLoading) {
                        Text(stringResource(R.string.common_cancel), color = colors.secondaryText)
                    }
                }
            }

            smsFailed && smsRateLimited -> {
                Text(
                    text = stringResource(R.string.mfa_rate_limited_body),
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                )
            }

            smsFailed -> {
                Text(
                    text = stringResource(
                        R.string.mfa_code_sent_to,
                        resolvedPhoneE164.ifBlank { maskedPhone },
                    ),
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSendCode(resolvedPhoneE164.ifBlank { null }) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                ) {
                    Text(stringResource(R.string.mfa_resend))
                }
                if (isEnroll) {
                    TextButton(onClick = { onSetEditingPhone(true) }, enabled = !isLoading) {
                        Text(stringResource(R.string.mfa_edit_number), color = TabSelected)
                    }
                }
            }

            showOtp -> {
                if (isEnroll && !smsSent && !smsFailed) {
                    CircularProgressIndicator(color = TabSelected)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.mfa_sending_code), color = colors.secondaryText)
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(R.string.mfa_sms_code)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onConfirm(code) },
                        enabled = !isLoading && code.length >= 6,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TabSelected),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(22.dp),
                            )
                        } else {
                            Text(
                                if (isEnroll) {
                                    stringResource(R.string.mfa_enable)
                                } else {
                                    stringResource(R.string.mfa_verify)
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (isEnroll) {
                        TextButton(onClick = { onSetEditingPhone(true) }, enabled = !isLoading) {
                            Text(stringResource(R.string.mfa_edit_number), color = TabSelected)
                        }
                    } else {
                        TextButton(onClick = { onSendCode(null) }, enabled = !isLoading) {
                            Text(stringResource(R.string.mfa_resend), color = TabSelected)
                        }
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank() && !(smsFailed && smsRateLimited)) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMessage, color = LikeRed, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        if (isLoading && waitingForEmail) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = TabSelected)
        }

        TextButton(onClick = onCancel) {
            Text(
                if (isEnroll) {
                    stringResource(R.string.mfa_sign_out_start_over)
                } else {
                    stringResource(R.string.common_cancel)
                },
                color = colors.secondaryText,
            )
        }
    }

    if (showCountryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCountryPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.background,
        ) {
            Text(
                text = stringResource(R.string.mfa_country_picker_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.text,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                items(PhoneCountry.all, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                country = item
                                showCountryPicker = false
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.flag, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(item.name, color = colors.text, modifier = Modifier.weight(1f))
                        Text(item.dialCode, color = colors.secondaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneEditorRow(
    country: PhoneCountry,
    nationalNumber: String,
    onNationalChange: (String) -> Unit,
    onPickCountry: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = country.displayLabel,
            color = colors.text,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .clickable(onClick = onPickCountry)
                .padding(horizontal = 12.dp, vertical = 16.dp),
        )
        OutlinedTextField(
            value = nationalNumber,
            onValueChange = onNationalChange,
            label = { Text(stringResource(R.string.mfa_phone_label)) },
            placeholder = { Text(stringResource(R.string.auth_phone_placeholder)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        )
    }
}
