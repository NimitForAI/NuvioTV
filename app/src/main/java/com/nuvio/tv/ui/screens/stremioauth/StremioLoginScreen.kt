@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.stremioauth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioTheme

// NOTE: TV Material3 has no text-field component, so we use the standard
// Material3 OutlinedTextField. It is focusable and works with a D-pad + the
// Android TV on-screen keyboard. Aliased to avoid clashing with tv.material3.
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField
import androidx.compose.material3.Text as M3Text

/**
 * Stremio account sign-in for Android TV.
 *
 * Email/password against Stremio's public API — works on sideloaded builds
 * with no client keys. On success, [onLoggedIn] fires; wire it to pop back to
 * the account/settings screen (or bootstrap addon sync).
 *
 * When [onSkip] is non-null, a "Skip for now" action is shown so the user can
 * enter the app without a Stremio account (mirrors Nuvio's original QR skip).
 * Pass null to make sign-in mandatory.
 */
@Composable
fun StremioLoginScreen(
    onLoggedIn: () -> Unit,
    onSkip: (() -> Unit)? = null,
    viewModel: StremioLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loggedIn) {
        if (uiState.loggedIn) onLoggedIn()
    }

    val emailFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { emailFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(
                    NuvioTheme.colors.BackgroundCard,
                    RoundedCornerShape(NuvioTheme.radii.xl)
                )
                .padding(NuvioTheme.spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.lg)
        ) {
            Text(
                text = if (uiState.isRegisterMode) "Create a Stremio account" else "Sign in to Stremio",
                style = MaterialTheme.typography.headlineSmall,
                color = NuvioTheme.colors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Use your Stremio account to sync your library, addons and continue watching.",
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center
            )

            M3OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { M3Text("Email") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocus)
            )

            M3OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { M3Text("Password") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioTheme.colors.Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(NuvioTheme.spacing.xs))

            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                border = ButtonDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(NuvioTheme.spacing.xxs, NuvioTheme.colors.FocusRing),
                        shape = RoundedCornerShape(NuvioTheme.radii.md)
                    )
                )
            ) {
                Text(
                    text = when {
                        uiState.isSubmitting -> "Please wait…"
                        uiState.isRegisterMode -> "Create account"
                        else -> "Sign in"
                    }
                )
            }

            Button(
                onClick = viewModel::toggleMode,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundElevated
                )
            ) {
                Text(
                    text = if (uiState.isRegisterMode)
                        "Have an account? Sign in"
                    else
                        "New to Stremio? Create an account"
                )
            }

            // "Skip for now" — only shown when the host allows entering the app
            // without a Stremio account (mirrors Nuvio's original QR skip).
            if (onSkip != null) {
                Button(
                    onClick = onSkip,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = "Skip for now",
                        color = NuvioTheme.extendedColors.textSecondary
                    )
                }
            }
        }
    }
}
