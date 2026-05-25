package at.gdev.contacts.ui.auth

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import at.gdev.contacts.data.network.ApiConfig

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.success) {
        if (state.success) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state.step) {
            LoginStep.Credentials -> CredentialsStep(
                state = state,
                viewModel = viewModel,
                onForgotPassword = { openUrl(context, ApiConfig.FORGOT_PASSWORD_URL) },
                onCreateAccount = onCreateAccount,
            )
            LoginStep.TwoFactor -> TwoFactorStep(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun CredentialsStep(
    state: LoginUiState,
    viewModel: LoginViewModel,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    Text("Contacts", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(8.dp))
    Text("Sign in to continue", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::setEmail,
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = viewModel::setPassword,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.error != null) {
        Spacer(Modifier.height(8.dp))
        Text(state.error, color = MaterialTheme.colorScheme.error)
    }

    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::submit,
        enabled = !state.submitting && state.email.isNotBlank() && state.password.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.submitting) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
        } else {
            Text("Sign in")
        }
    }

    Spacer(Modifier.height(16.dp))
    TextButton(onClick = onForgotPassword) { Text("Forgot password?") }
    TextButton(onClick = onCreateAccount) { Text("Create an account") }
}

@Composable
private fun TwoFactorStep(state: LoginUiState, viewModel: LoginViewModel) {
    val usingRecovery = state.useRecoveryCode
    Text("Two-factor authentication", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        text = if (usingRecovery) {
            "Enter one of your recovery codes."
        } else {
            "Enter the 6-digit code from your authenticator app."
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(24.dp))

    OutlinedTextField(
        value = state.twoFactorCode,
        onValueChange = viewModel::setTwoFactorCode,
        label = { Text(if (usingRecovery) "Recovery code" else "Authentication code") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (usingRecovery) KeyboardType.Text else KeyboardType.NumberPassword,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.error != null) {
        Spacer(Modifier.height(8.dp))
        Text(state.error, color = MaterialTheme.colorScheme.error)
    }

    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::submit,
        enabled = !state.submitting && state.twoFactorCode.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.submitting) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
        } else {
            Text("Verify")
        }
    }

    Spacer(Modifier.height(8.dp))
    TextButton(onClick = viewModel::toggleRecoveryCode) {
        Text(if (usingRecovery) "Use authenticator code" else "Use a recovery code")
    }
    TextButton(onClick = viewModel::cancelTwoFactor) {
        Text("Back to sign in")
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
