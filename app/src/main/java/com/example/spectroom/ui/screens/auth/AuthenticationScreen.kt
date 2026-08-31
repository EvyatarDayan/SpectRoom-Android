package com.example.spectroom.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.R
import com.example.spectroom.ui.theme.LikeRed
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import com.example.spectroom.ui.theme.TabSelected

@Composable
fun AuthenticationScreen(
    isLoading: Boolean,
    errorMessage: String?,
    noticeMessage: String? = null,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (name: String, email: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onClearError: () -> Unit,
) {
    val colors = LocalSpectRoomColors.current
    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (isSignUp) R.string.auth_create_account else R.string.auth_welcome_back,
            ),
            color = colors.secondaryText,
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; onClearError() },
                label = { Text(stringResource(R.string.auth_full_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onClearError() },
            label = { Text(stringResource(R.string.auth_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onClearError() },
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        if (!isSignUp) {
            TextButton(
                onClick = { onForgotPassword(email) },
                enabled = !isLoading && email.contains("@"),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.auth_forgot_password), color = TabSelected)
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage, color = LikeRed, fontSize = 13.sp)
        }
        if (!noticeMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = noticeMessage, color = TabSelected, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isSignUp) onSignUp(name, email, password) else onSignIn(email, password)
            },
            enabled = !isLoading && email.isNotBlank() && password.length >= 6 &&
                (!isSignUp || name.trim().split(" ").size >= 2),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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
                    stringResource(if (isSignUp) R.string.auth_sign_up else R.string.auth_sign_in),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        TextButton(onClick = { isSignUp = !isSignUp; onClearError() }) {
            Text(
                text = stringResource(
                    if (isSignUp) R.string.auth_have_account else R.string.auth_no_account,
                ),
                color = TabSelected,
            )
        }
    }
}
