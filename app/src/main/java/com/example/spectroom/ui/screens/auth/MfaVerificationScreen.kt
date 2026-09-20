package com.example.spectroom.ui.screens.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.spectroom.R
import com.example.spectroom.data.model.PhoneCountry
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected
import com.example.spectroom.viewmodel.MfaUiMode
import kotlinx.coroutines.delay

private val FieldCorner = 12.dp
private val FieldFill = Color.Gray.copy(alpha = 0.10f)
private val FieldStroke = Color.Gray.copy(alpha = 0.30f)
private const val SmsResendCooldownMs = 45_000L

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
    popupTitle: String? = null,
    onSendVerificationEmail: () -> Unit,
    onCheckEmailVerified: (manual: Boolean) -> Unit,
    onResolvePhone: () -> Unit,
    onSaveMissingPhone: (phoneE164: String) -> Unit,
    onSaveEditedPhone: (phoneE164: String) -> Unit,
    onSetEditingPhone: (Boolean) -> Unit,
    onSendCode: (phoneE164: String?) -> Unit,
    onConfirm: (code: String) -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    val isEnroll = mode == MfaUiMode.ENROLL
    var country by remember { mutableStateOf(PhoneCountry.Sweden) }
    var nationalNumber by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var showEmailCelebration by remember { mutableStateOf(false) }
    var sawUnverifiedEmail by remember { mutableStateOf(!emailVerified) }
    var emailCelebrationDone by remember { mutableStateOf(emailVerified) }
    var emailResendSeconds by remember { mutableIntStateOf(0) }
    var lastSmsResendAt by remember { mutableLongStateOf(0L) }
    var smsResendToast by remember { mutableStateOf("") }
    var codeFocused by remember { mutableStateOf(false) }
    val codeFocusRequester = remember { FocusRequester() }
    val e164 = PhoneCountry.buildE164(country, nationalNumber)
    val phoneFallback = stringResource(R.string.mfa_phone_fallback)
    val displayedPhone = when {
        maskedPhone.isNotBlank() -> maskedPhone
        resolvedPhoneE164.isNotBlank() -> resolvedPhoneE164
        else -> phoneFallback
    }

    // iOS step order: missing phone → email → celebration → edit phone → SMS fail → sending → OTP
    val phoneMissing = isEnroll && resolvedPhoneE164.isBlank()
    val waitingForEmail = isEnroll &&
        resolvedPhoneE164.isNotBlank() &&
        !emailVerified &&
        !showEmailCelebration
    val onOtpStep = (!isEnroll) || (
        emailVerified &&
            !showEmailCelebration &&
            resolvedPhoneE164.isNotBlank() &&
            !editingPhone &&
            (smsSent || smsFailed)
        )
    val showSending = isEnroll &&
        emailVerified &&
        !showEmailCelebration &&
        resolvedPhoneE164.isNotBlank() &&
        !editingPhone &&
        !smsSent &&
        !smsFailed

    val headerTitle = when {
        waitingForEmail || showEmailCelebration -> stringResource(R.string.mfa_email_title)
        onOtpStep && !smsFailed -> stringResource(R.string.mfa_code_title)
        isEnroll && editingPhone -> stringResource(R.string.mfa_phone_edit_title)
        else -> stringResource(R.string.mfa_title)
    }
    val subtitleText = when {
        phoneMissing -> stringResource(R.string.mfa_phone_missing_title)
        waitingForEmail || showEmailCelebration -> ""
        isEnroll && editingPhone -> stringResource(R.string.mfa_phone_edit_subtitle)
        isEnroll && smsFailed -> stringResource(
            if (smsRateLimited) {
                R.string.mfa_error_rate_limited_subtitle
            } else {
                R.string.mfa_sending_failed_subtitle
            },
        )
        showSending -> stringResource(R.string.mfa_sending_subtitle)
        else -> ""
    }
    val showsLockIcon = !waitingForEmail && !showEmailCelebration

    LaunchedEffect(Unit) {
        if (isEnroll) onResolvePhone()
    }

    LaunchedEffect(waitingForEmail) {
        if (!waitingForEmail) return@LaunchedEffect
        emailResendSeconds = 30
        while (waitingForEmail) {
            delay(3_000)
            onCheckEmailVerified(false)
        }
    }

    LaunchedEffect(emailResendSeconds, waitingForEmail) {
        if (!waitingForEmail || emailResendSeconds <= 0) return@LaunchedEffect
        while (emailResendSeconds > 0 && waitingForEmail) {
            delay(1_000)
            emailResendSeconds -= 1
        }
    }

    LaunchedEffect(emailVerified) {
        if (emailVerified && sawUnverifiedEmail && isEnroll) {
            showEmailCelebration = true
            delay(1_250)
            showEmailCelebration = false
            sawUnverifiedEmail = false
            emailCelebrationDone = true
        } else if (emailVerified) {
            sawUnverifiedEmail = false
            emailCelebrationDone = true
        }
    }

    LaunchedEffect(
        emailVerified,
        emailCelebrationDone,
        resolvedPhoneE164,
        smsSent,
        smsFailed,
        editingPhone,
        isLoading,
        showEmailCelebration,
    ) {
        if (!isEnroll || !emailVerified || !emailCelebrationDone) return@LaunchedEffect
        if (showEmailCelebration || editingPhone) return@LaunchedEffect
        if (resolvedPhoneE164.isBlank() || smsSent || smsFailed || isLoading) return@LaunchedEffect
        onSendCode(resolvedPhoneE164)
    }

    LaunchedEffect(onOtpStep, smsSent) {
        if (onOtpStep && smsSent) {
            delay(100)
            runCatching { codeFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(smsSent) {
        if (smsSent && lastSmsResendAt == 0L) {
            lastSmsResendAt = System.currentTimeMillis()
        }
    }

    LaunchedEffect(smsResendToast) {
        if (smsResendToast.isEmpty()) return@LaunchedEffect
        delay(2_500)
        smsResendToast = ""
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, waitingForEmail) {
        if (!waitingForEmail) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onCheckEmailVerified(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = headerTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )
            if (showsLockIcon) {
                Spacer(modifier = Modifier.height(14.dp))
                Image(
                    painter = painterResource(R.drawable.ic_mfa_lock),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            if (subtitleText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subtitleText,
                    color = colors.secondaryText,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                phoneMissing -> {
                    MfaPhoneEditor(
                        country = country,
                        nationalNumber = nationalNumber,
                        onNationalChange = { nationalNumber = it.filter(Char::isDigit).take(15) },
                        onPickCountry = { showCountryPicker = true },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.auth_phone_helper),
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MfaPrimaryButton(
                        title = stringResource(R.string.mfa_phone_missing_save),
                        enabled = !isLoading && e164 != null,
                        loading = isLoading,
                        onClick = { e164?.let(onSaveMissingPhone) },
                    )
                }

                showEmailCelebration -> {
                    Image(
                        painter = painterResource(R.drawable.ic_mfa_email_verified),
                        contentDescription = null,
                        modifier = Modifier.size(76.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.mfa_email_verified),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                }

                waitingForEmail -> {
                    val pulse = rememberInfiniteTransition(label = "emailPulse")
                    val scale by pulse.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1150),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "emailScale",
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_mfa_envelope_circle),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(scale),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = stringResource(
                            R.string.mfa_email_body,
                            emailAddress.ifBlank { stringResource(R.string.mfa_email_fallback) },
                        ),
                        color = colors.secondaryText,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    MfaPrimaryButton(
                        title = stringResource(R.string.mfa_email_check),
                        enabled = !isLoading,
                        loading = isLoading,
                        onClick = { onCheckEmailVerified(true) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (emailResendSeconds > 0) {
                        Text(
                            text = stringResource(R.string.mfa_email_resend),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.mfa_email_resend_wait_hint),
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.mfa_email_resend),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TabSelected,
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                onSendVerificationEmail()
                                emailResendSeconds = 30
                            },
                        )
                    }
                }

                isEnroll && editingPhone -> {
                    MfaPhoneEditor(
                        country = country,
                        nationalNumber = nationalNumber,
                        onNationalChange = { nationalNumber = it.filter(Char::isDigit).take(15) },
                        onPickCountry = { showCountryPicker = true },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.auth_phone_helper),
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MfaPrimaryButton(
                        title = stringResource(R.string.mfa_phone_edit_save),
                        enabled = !isLoading && e164 != null,
                        loading = isLoading,
                        onClick = { e164?.let(onSaveEditedPhone) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.mfa_phone_edit_cancel),
                        fontSize = 14.sp,
                        color = colors.secondaryText,
                        modifier = Modifier.clickable(enabled = !isLoading) {
                            onSetEditingPhone(false)
                        },
                    )
                }

                isEnroll && smsFailed -> {
                    if (smsRateLimited) {
                        Text(
                            text = stringResource(R.string.mfa_error_rate_limited),
                            color = colors.secondaryText,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.mfa_error_sms_unavailable),
                            color = colors.secondaryText,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.mfa_phone_using, displayedPhone),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = colors.text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MfaPrimaryButton(
                            title = stringResource(R.string.mfa_resend),
                            enabled = !isLoading,
                            loading = isLoading,
                            onClick = { onSendCode(resolvedPhoneE164.ifBlank { null }) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.mfa_edit_number),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TabSelected,
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                applyPhoneFields(resolvedPhoneE164) { c, n ->
                                    country = c
                                    nationalNumber = n
                                }
                                onSetEditingPhone(true)
                            },
                        )
                    }
                }

                showSending -> {
                    CircularProgressIndicator(color = TabSelected)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.mfa_sending_body),
                        color = colors.secondaryText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.mfa_code_sent_to),
                        color = colors.secondaryText,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayedPhone,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = colors.text,
                        textAlign = TextAlign.Center,
                    )
                    if (isEnroll) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.mfa_edit_number),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TabSelected,
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                applyPhoneFields(resolvedPhoneE164) { c, n ->
                                    country = c
                                    nationalNumber = n
                                }
                                onSetEditingPhone(true)
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    OtpDigitBoxes(
                        code = code,
                        focused = codeFocused,
                        focusRequester = codeFocusRequester,
                        onCodeChange = { code = it.filter(Char::isDigit).take(6) },
                        onFocusChange = { codeFocused = it },
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    MfaPrimaryButton(
                        title = stringResource(R.string.mfa_verify),
                        enabled = !isLoading && code.length >= 6,
                        loading = isLoading,
                        onClick = { onConfirm(code) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.mfa_resend_link),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TabSelected,
                        modifier = Modifier.clickable(enabled = !isLoading) {
                            val now = System.currentTimeMillis()
                            if (lastSmsResendAt > 0 && now - lastSmsResendAt < SmsResendCooldownMs) {
                                smsResendToast = "wait"
                            } else {
                                lastSmsResendAt = now
                                code = ""
                                onSendCode(if (isEnroll) resolvedPhoneE164.ifBlank { null } else null)
                                smsResendToast = "sent"
                            }
                        },
                    )
                    if (smsResendToast.isNotEmpty()) {
                        val toastText = when (smsResendToast) {
                            "wait" -> stringResource(R.string.mfa_resend_wait)
                            else -> stringResource(R.string.mfa_resend_sent)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = toastText,
                            fontSize = 13.sp,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (!isEnroll) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.mfa_back_to_sign_in),
                            fontSize = 14.sp,
                            color = colors.secondaryText,
                            modifier = Modifier.clickable(enabled = !isLoading, onClick = onCancel),
                        )
                    }
                }
            }

            if (isEnroll) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.mfa_sign_out),
                    fontSize = 14.sp,
                    color = colors.secondaryText,
                    modifier = Modifier
                        .clickable(
                            enabled = !isLoading && !showEmailCelebration,
                            onClick = onCancel,
                        )
                        .padding(bottom = 20.dp),
                )
            }
        }
        }

        if (!errorMessage.isNullOrBlank()) {
            AuthMessageOverlay(
                message = errorMessage,
                title = popupTitle?.takeIf { it.isNotBlank() },
                onDismiss = onClearError,
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
private fun MfaPrimaryButton(
    title: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) TabSelected else Color.Gray,
            disabledContainerColor = Color.Gray,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun MfaPhoneEditor(
    country: PhoneCountry,
    nationalNumber: String,
    onNationalChange: (String) -> Unit,
    onPickCountry: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(FieldCorner))
                .background(FieldFill)
                .border(1.dp, FieldStroke, RoundedCornerShape(FieldCorner))
                .clickable(onClick = onPickCountry)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(country.flag, fontSize = 18.sp)
            Text(
                text = country.dialCode,
                color = colors.text,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(FieldCorner))
                .background(FieldFill)
                .border(1.dp, FieldStroke, RoundedCornerShape(FieldCorner))
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            if (nationalNumber.isEmpty()) {
                Text(
                    text = stringResource(R.string.auth_phone_placeholder),
                    color = colors.secondaryText,
                    fontSize = 16.sp,
                )
            }
            BasicTextField(
                value = nationalNumber,
                onValueChange = onNationalChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
                cursorBrush = SolidColor(TabSelected),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OtpDigitBoxes(
    code: String,
    focused: Boolean,
    focusRequester: FocusRequester,
    onCodeChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                focusRequester.requestFocus()
            },
    ) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChange(it.isFocused) },
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(6) { index ->
                val digit = code.getOrNull(index)?.toString().orEmpty()
                val active = focused && code.length == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FieldFill)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) TabSelected else FieldStroke,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (digit.isEmpty()) " " else digit,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.text,
                    )
                }
            }
        }
    }
}

private fun applyPhoneFields(e164: String, apply: (PhoneCountry, String) -> Unit) {
    if (!e164.startsWith("+")) return
    val match = PhoneCountry.all
        .sortedByDescending { it.dialCode.length }
        .firstOrNull { e164.startsWith(it.dialCode) }
        ?: return
    apply(match, e164.removePrefix(match.dialCode))
}
