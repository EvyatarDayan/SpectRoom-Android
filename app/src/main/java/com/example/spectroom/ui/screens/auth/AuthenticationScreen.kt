package com.example.spectroom.ui.screens.auth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.data.model.PhoneCountry
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

private val FieldCorner = 12.dp
private val FieldFill = Color.Gray.copy(alpha = 0.10f)
private val FieldStroke = Color.Gray.copy(alpha = 0.30f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    isLoading: Boolean,
    errorMessage: String?,
    noticeMessage: String? = null,
    initialEmail: String = "",
    initialPassword: String = "",
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (name: String, email: String, password: String, phoneE164: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onClearError: () -> Unit,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
) {
    val colors = LocalSpectRoomColors.current
    val focusManager = LocalFocusManager.current
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneCountry by remember { mutableStateOf(PhoneCountry.Sweden) }
    var phoneNumber by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var localPopupMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialEmail) {
        if (initialEmail.isNotBlank() && email.isBlank()) {
            email = initialEmail
        }
    }
    LaunchedEffect(initialPassword) {
        if (initialPassword.isNotBlank() && password.isBlank()) {
            password = initialPassword
        }
    }

    val popupMessage = localPopupMessage ?: errorMessage ?: noticeMessage
    val errorName = stringResource(R.string.auth_error_name)
    val errorEmail = stringResource(R.string.auth_error_email_format)
    val errorPhone = stringResource(R.string.auth_error_phone_invalid)
    val errorPassword = stringResource(R.string.auth_error_password_match)
    val errorEmailRequired = stringResource(R.string.auth_error_email_required)
    val resetSentFormat = stringResource(R.string.auth_reset_password_sent)

    fun clearErrors() {
        localPopupMessage = null
        onClearError()
    }

    fun switchMode(login: Boolean) {
        isLoginMode = login
        clearErrors()
        focusManager.clearFocus()
    }

    fun submit() {
        if (isLoading) return
        clearErrors()
        if (isLoginMode) {
            onSignIn(email, password)
            return
        }
        val trimmedName = name.trim()
        if (trimmedName.split(Regex("\\s+")).filter { it.isNotEmpty() }.size < 2) {
            localPopupMessage = errorName
            return
        }
        val trimmedEmail = email.trim()
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            localPopupMessage = errorEmail
            return
        }
        val digits = phoneNumber.filter { it.isDigit() }
        val e164 = PhoneCountry.buildE164(phoneCountry, digits)
        if (e164 == null) {
            localPopupMessage = errorPhone
            return
        }
        if (password.isEmpty() || confirmPassword.isEmpty() || password != confirmPassword) {
            localPopupMessage = errorPassword
            return
        }
        name = trimmedName
        email = trimmedEmail
        phoneNumber = digits
        onSignUp(trimmedName, trimmedEmail, password, e164)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(28.dp))
                AuthModeSegmentedControl(
                    isLoginMode = isLoginMode,
                    onSelectLogin = { switchMode(true) },
                    onSelectSignUp = { switchMode(false) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!isLoginMode) {
                AuthLabeledField(label = stringResource(R.string.auth_full_name)) {
                    AuthTextField(
                        value = name,
                        onValueChange = { name = it; clearErrors() },
                        placeholder = stringResource(R.string.auth_name_placeholder),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    )
                }
            }

                    AuthLabeledField(label = stringResource(R.string.auth_email)) {
                        AuthTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                onEmailChange(it)
                                clearErrors()
                            },
                            placeholder = stringResource(R.string.auth_email_placeholder),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )
            }

            if (!isLoginMode) {
                AuthLabeledField(label = stringResource(R.string.auth_phone)) {
                    PhoneNumberRow(
                        country = phoneCountry,
                        nationalNumber = phoneNumber,
                        onNationalChange = { value ->
                            phoneNumber = value.filter { it.isDigit() }
                            clearErrors()
                        },
                        onPickCountry = { showCountryPicker = true },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.auth_phone_helper),
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                    )
                }
            }

            AuthLabeledField(label = stringResource(R.string.auth_password)) {
                AuthPasswordField(
                    value = password,
                    onValueChange = {
                        password = it
                        onPasswordChange(it)
                        clearErrors()
                    },
                    placeholder = stringResource(R.string.auth_password_placeholder),
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                    imeAction = if (isLoginMode) ImeAction.Go else ImeAction.Next,
                    onImeAction = {
                        if (isLoginMode) submit() else focusManager.moveFocus(FocusDirection.Down)
                    },
                )
            }

            if (!isLoginMode) {
                AuthLabeledField(label = stringResource(R.string.auth_confirm_password)) {
                    AuthPasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; clearErrors() },
                        placeholder = stringResource(R.string.auth_confirm_password_placeholder),
                        visible = confirmPasswordVisible,
                        onToggleVisible = { confirmPasswordVisible = !confirmPasswordVisible },
                        imeAction = ImeAction.Go,
                        onImeAction = { submit() },
                    )
                }
            }

            Button(
                onClick = { submit() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoading) Color.Gray else TabSelected,
                    disabledContainerColor = if (isLoading) Color.Gray else TabSelected,
                    contentColor = Color.White,
                    disabledContentColor = Color.White,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (isLoginMode) R.string.auth_sign_in else R.string.auth_sign_up,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isLoginMode) {
                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        color = TabSelected,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(enabled = !isLoading) {
                            clearErrors()
                            if (email.isBlank()) {
                                localPopupMessage = errorEmailRequired
                            } else {
                                // iOS shows the success alert immediately while the reset runs.
                                localPopupMessage = String.format(resetSentFormat, email.trim())
                                onForgotPassword(email)
                            }
                        },
                    )
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = stringResource(R.string.auth_no_account) + " ",
                            color = colors.secondaryText,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = stringResource(R.string.auth_sign_up),
                            color = TabSelected,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { switchMode(false) },
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = stringResource(R.string.auth_have_account) + " ",
                            color = colors.secondaryText,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = stringResource(R.string.auth_sign_in),
                            color = TabSelected,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { switchMode(true) },
                        )
                    }
                }
            }
        }
        }

        if (!popupMessage.isNullOrBlank()) {
            AuthMessageOverlay(
                message = popupMessage.orEmpty(),
                onDismiss = { clearErrors() },
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
                                phoneCountry = item
                                showCountryPicker = false
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.flag, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(item.name, color = colors.text, modifier = Modifier.weight(1f))
                        Text(item.dialCode, color = colors.secondaryText)
                        if (item.id == phoneCountry.id) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = TabSelected,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeSegmentedControl(
    isLoginMode: Boolean,
    onSelectLogin: () -> Unit,
    onSelectSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Gray.copy(alpha = 0.16f))
            .padding(3.dp),
    ) {
        AuthSegment(
            text = stringResource(R.string.auth_sign_in),
            selected = isLoginMode,
            onClick = onSelectLogin,
            modifier = Modifier.weight(1f),
            selectedColor = colors.card,
            textColor = colors.text,
        )
        AuthSegment(
            text = stringResource(R.string.auth_sign_up),
            selected = !isLoginMode,
            onClick = onSelectSignUp,
            modifier = Modifier.weight(1f),
            selectedColor = colors.card,
            textColor = colors.text,
        )
    }
}

@Composable
private fun AuthSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color,
    textColor: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = textColor,
        )
    }
}

@Composable
private fun AuthLabeledField(
    label: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = colors.text,
        )
        content()
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = LocalSpectRoomColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCorner))
            .background(FieldFill)
            .border(1.dp, FieldStroke, RoundedCornerShape(FieldCorner))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = colors.secondaryText, fontSize = 16.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
            cursorBrush = SolidColor(TabSelected),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCorner))
            .background(FieldFill)
            .border(1.dp, FieldStroke, RoundedCornerShape(FieldCorner)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            if (value.isEmpty()) {
                Text(placeholder, color = colors.secondaryText, fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
                cursorBrush = SolidColor(TabSelected),
                visualTransformation = if (visible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { onImeAction() },
                    onDone = { onImeAction() },
                    onNext = { onImeAction() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(onClick = onToggleVisible) {
            Icon(
                imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
                tint = Color.Gray,
            )
        }
    }
}

@Composable
private fun PhoneNumberRow(
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
