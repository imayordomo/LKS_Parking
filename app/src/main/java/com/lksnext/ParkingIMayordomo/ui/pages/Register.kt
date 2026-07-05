package com.lksnext.ParkingIMayordomo.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.ui.viewmodel.RegisterViewModel
import com.lksnext.ParkingIMayordomo.utils.LocaleManager
import com.lksnext.ParkingIMayordomo.utils.TestTags

@Composable
fun Register(
    viewModel: RegisterViewModel,
    onBackToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    
    val isLoading by viewModel.loading.collectAsState()
    val apiErrorResId by viewModel.errorResId.collectAsState()
    var validationErrorResId by rememberSaveable { mutableStateOf<Int?>(null) }
    
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val systemContext = remember(context) { LocaleManager.getSystemLocaleContext(context) }
    CompositionLocalProvider(LocalContext provides systemContext) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(scrollState),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.landing_title),
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Light
                )
            }

            Text(
                text = stringResource(R.string.create_account),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            RegisterErrorBanner(
                apiErrorResId = apiErrorResId,
                validationErrorResId = validationErrorResId
            )

            RegisterFormFields(
                name = name,
                onNameChange = { name = it; validationErrorResId = null },
                email = email,
                onEmailChange = { email = it; validationErrorResId = null },
                password = password,
                onPasswordChange = { password = it; validationErrorResId = null },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it; validationErrorResId = null },
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            RegisterButton(
                isLoading = isLoading,
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedEmail = email.trim()
                    
                    when {
                        trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                            validationErrorResId = R.string.error_required_fields
                        !trimmedEmail.endsWith("@lksnext.com") ->
                            validationErrorResId = R.string.error_corporate_only
                        password != confirmPassword ->
                            validationErrorResId = R.string.error_passwords_dont_match
                        password.length < 8 ->
                            validationErrorResId = R.string.error_password_too_short
                        !password.any { it.isUpperCase() } || !password.any { it.isDigit() } ->
                            validationErrorResId = R.string.error_password_complexity
                        else -> viewModel.register(trimmedName, trimmedEmail, password, onRegisterSuccess)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.already_have_account),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light
                )
                TextButton(onClick = onBackToLogin, modifier = Modifier.testTag(TestTags.REGISTER_LOGIN_LINK), enabled = !isLoading) {
                    Text(
                        text = stringResource(R.string.login_link),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun RegisterErrorBanner(
    apiErrorResId: Int?,
    validationErrorResId: Int?
) {
    if (apiErrorResId != null || validationErrorResId != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag(TestTags.REGISTER_ERROR_MESSAGE),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Text(
                text = stringResource(apiErrorResId ?: validationErrorResId ?: 0),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun RegisterFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isLoading: Boolean
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.full_name_label)) },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.REGISTER_NAME_FIELD),
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true,
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.email_label)) },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.REGISTER_EMAIL_FIELD),
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true,
        supportingText = { Text(stringResource(R.string.corporate_email_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.password_label)) },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.REGISTER_PASSWORD_FIELD),
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = !isLoading,
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = onPasswordVisibilityToggle, modifier = Modifier.testTag(TestTags.REGISTER_TOGGLE_PASSWORD)) {
                Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = { Text(stringResource(R.string.confirm_password_label)) },
        modifier = Modifier.fillMaxWidth().testTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD),
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = !isLoading,
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = onPasswordVisibilityToggle, modifier = Modifier.testTag(TestTags.REGISTER_TOGGLE_CONFIRM_PASSWORD)) {
                Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    )
}

@Composable
private fun RegisterButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(TestTags.REGISTER_REGISTER_BUTTON),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(4.dp),
        enabled = !isLoading,
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.register_loading), fontSize = 16.sp)
        } else {
            Text(text = stringResource(R.string.register_button), fontSize = 16.sp)
        }
    }
}
