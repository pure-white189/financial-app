package com.example.myapplication

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPage(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val resetEmailState by authViewModel.resetEmailState.collectAsStateWithLifecycle()

    var isLoginMode by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }
    var isAuthErrorDismissed by rememberSaveable { mutableStateOf(false) }
    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var resetEmailInput by rememberSaveable { mutableStateOf("") }
    var showResetSuccessMessage by rememberSaveable { mutableStateOf(false) }

    val authErrorMessage = (authState as? AuthState.Error)?.message
    val isEmailFormatValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val emailError = when {
        !showValidationErrors -> null
        email.isBlank() -> "Email cannot be empty"
        !isEmailFormatValid -> "Please enter a valid email address"
        else -> null
    }
    val passwordError = if (showValidationErrors && password.length < 6) {
        "Password must be at least 6 characters"
    } else {
        null
    }
    val confirmPasswordError = if (!isLoginMode && showValidationErrors && confirmPassword != password) {
        "Passwords do not match"
    } else {
        null
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(authErrorMessage) {
        if (authErrorMessage != null) {
            isAuthErrorDismissed = false
        }
    }

    LaunchedEffect(resetEmailState) {
        if (resetEmailState == "sent") {
            showForgotPasswordDialog = false
            showResetSuccessMessage = true
            authViewModel.clearResetEmailState()
        }
    }

    val isLoading = authState is AuthState.Loading

    Scaffold(
        topBar = {
            if (onDismiss != null) {
                TopAppBar(
                    title = { Text("") },
                    actions = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = "财务管家",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "登录后开始管理你的资产与消费",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it.trim()
                if (showValidationErrors) showValidationErrors = false
            },
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null,
            enabled = !isLoading,
            supportingText = {
                if (emailError != null) {
                    Text(text = emailError, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (showValidationErrors) showValidationErrors = false
            },
            label = { Text("Password") },
            singleLine = true,
            enabled = !isLoading,
            isError = passwordError != null,
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Text(if (isPasswordVisible) "Hide" else "Show")
                }
            },
            supportingText = {
                if (passwordError != null) {
                    Text(text = passwordError, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (isLoginMode) {
            TextButton(
                onClick = {
                    resetEmailInput = email
                    showForgotPasswordDialog = true
                    showResetSuccessMessage = false
                    authViewModel.clearResetEmailState()
                },
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot password?")
            }
        }

        if (!isLoginMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (showValidationErrors) showValidationErrors = false
                },
                label = { Text("Confirm Password") },
                singleLine = true,
                enabled = !isLoading,
                isError = confirmPasswordError != null,
                visualTransformation = if (isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Text(if (isConfirmPasswordVisible) "Hide" else "Show")
                    }
                },
                supportingText = {
                    if (confirmPasswordError != null) {
                        Text(text = confirmPasswordError, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                showValidationErrors = true
                val hasEmailError = email.isBlank()
                val hasInvalidEmailFormat = !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                val hasPasswordError = password.length < 6
                val hasConfirmPasswordError = !isLoginMode && confirmPassword != password

                if (!hasEmailError && !hasInvalidEmailFormat && !hasPasswordError && !hasConfirmPasswordError) {
                    if (isLoginMode) {
                        authViewModel.signInWithEmail(email, password)
                    } else {
                        authViewModel.signUpWithEmail(email, password)
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoginMode) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoogleSignInClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue with Google")
        }

        if (!authErrorMessage.isNullOrBlank() && !isAuthErrorDismissed) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = authErrorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isAuthErrorDismissed = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        if (showResetSuccessMessage) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reset email sent. Check your inbox.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                isLoginMode = !isLoginMode
                showValidationErrors = false
                confirmPassword = ""
                isConfirmPasswordVisible = false
            },
            enabled = !isLoading
        ) {
            Text(
                if (isLoginMode) {
                    "Don't have an account? Register"
                } else {
                    "Already have an account? Login"
                }
            )
        }

        TextButton(
            onClick = onContinueAsGuest,
            enabled = !isLoading
        ) {
            Text("Continue without account")
        }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }

    if (showForgotPasswordDialog) {
        val resetError = resetEmailState?.takeIf { it.startsWith("error:") }
            ?.removePrefix("error:")
            ?.trim()

        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                authViewModel.clearResetEmailState()
            },
            title = { Text("Reset Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it.trim() },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!resetError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetSuccessMessage = false
                        authViewModel.sendPasswordResetEmail(resetEmailInput)
                    }
                ) {
                    Text("Send reset email")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        authViewModel.clearResetEmailState()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

