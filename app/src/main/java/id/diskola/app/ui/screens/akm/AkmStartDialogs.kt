package id.diskola.app.ui.screens.akm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.AkmSessionRules

/**
 * The gates a student passes between tapping "Mulai" and the exam opening, in legacy order (`AkmSessionRules.Gate`): the password
 * (when asked), the terms, and — for a strict exam only — the online check. The priming screen comes after these.
 */
enum class AkmStartDialog { Password, Terms, NeedInternet }

@Composable
fun AkmStartDialogs(
    dialog: AkmStartDialog?,
    checking: Boolean,
    passwordError: String?,
    /** Strict mode (screen pinning) applies: the terms say so and the exam cannot be started offline. */
    strict: Boolean,
    onCheckPassword: (String) -> Unit,
    onDismiss: () -> Unit,
    onTermsAccepted: () -> Unit,
) {
    when (dialog) {
        AkmStartDialog.Password -> PasswordDialog(
            onDismiss = onDismiss,
            checking = checking,
            error = passwordError,
            onCheck = onCheckPassword,
        )

        AkmStartDialog.Terms -> AppDialog(
            onDismiss = onDismiss,
            title = AkmSessionRules.TERMS_TITLE,
            body = AkmSessionRules.termsBody(strict),
            primaryButtonText = AkmSessionRules.TERMS_OK,
            onPrimaryClick = onTermsAccepted,
        )

        AkmStartDialog.NeedInternet -> AppDialog(
            onDismiss = onDismiss,
            title = AkmSessionRules.OFFLINE_TITLE,
            body = AkmSessionRules.OFFLINE_BODY,
            primaryButtonText = AkmSessionRules.OFFLINE_OK,
            onPrimaryClick = onDismiss,
        )

        null -> Unit
    }
}

@Composable
private fun PasswordDialog(
    onDismiss: () -> Unit,
    checking: Boolean,
    error: String?,
    onCheck: (String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }

    AppDialog(
        onDismiss = onDismiss,
        title = "Password Ujian",
        body = "Masukkan password yang dibagikan pengawas di ruang ujian.",
        primaryButtonText = "Lanjut",
        onPrimaryClick = { if (password.isNotBlank()) onCheck(password) },
        primaryLoading = checking,
        secondaryButtonText = "Batal",
        onSecondaryClick = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password ujian",
                    enabled = !checking,
                )
                if (error != null) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
                if (checking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            "Memeriksa password…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        },
    )
}
