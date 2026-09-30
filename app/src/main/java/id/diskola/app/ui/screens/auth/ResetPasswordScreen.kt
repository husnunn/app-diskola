package id.diskola.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppCard
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.BannerError
import id.diskola.app.ui.components.CardVariant
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.ResetPasswordViewModel
import kotlinx.coroutines.delay

/** `FormResetPage` — doc `02-auth-login-sesi.md` §7. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onSent: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val sent by viewModel.sent.collectAsStateWithLifecycle()
    val sentEmail by viewModel.sentEmail.collectAsStateWithLifecycle()

    LaunchedEffect(sent) {
        if (sent) onSent(sentEmail)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Reset Password") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) } },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                "Kami akan mengirimkan link pemulihan ke email terdaftar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (errorMessage.isNotBlank()) {
                BannerError(message = errorMessage, onDismiss = { viewModel.clearError() })
            }

            AppCard(variant = CardVariant.Outlined) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    AppTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "email pemulihan".uppercase(),
                        placeholder = "Ketikkan email",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                    )
                    AppButton(
                        text = "kirim sekarang",
                        onClick = { viewModel.sendResetLink(email) },
                        enabled = email.length > 3,
                        loading = loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** `SuccessResetPage` — doc §7: 3-minute countdown, "kirim ulang" disabled only *after* the first
 * resend (gap `02a`/audit correction — the very first countdown does **not** disable the button). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordSentScreen(
    email: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    var remainingSeconds by remember { mutableIntStateOf(180) }
    var hasResendedOnce by remember { mutableStateOf(false) }
    var counting by remember { mutableStateOf(true) }

    LaunchedEffect(counting, remainingSeconds) {
        if (counting && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        } else if (remainingSeconds <= 0) {
            counting = false
        }
    }

    val buttonLabel = when {
        !hasResendedOnce -> "kirim ulang"
        counting -> "kirim ulang ${remainingSeconds / 60}:${remainingSeconds % 60}"
        else -> "kirim ulang"
    }
    val buttonEnabled = !hasResendedOnce || !counting

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Reset Password") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) } },
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(ScreenHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.size(Spacing.lg))
            Text("link pemulihan terkirim".uppercase(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.size(Spacing.sm))
            val annotated = remember(email) {
                androidx.compose.ui.text.buildAnnotatedString {
                    append("Link reset password telah dikirimkan, cek email ")
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { append(email) }
                }
            }
            Text(annotated, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.size(Spacing.xl))
            AppButton(
                text = buttonLabel,
                enabled = buttonEnabled,
                onClick = {
                    viewModel.resend(email)
                    hasResendedOnce = true
                    remainingSeconds = 180
                    counting = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
