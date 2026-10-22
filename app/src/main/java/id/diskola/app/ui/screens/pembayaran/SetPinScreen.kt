package id.diskola.app.ui.screens.pembayaran

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.AppTextField
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.ErrorState
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.PaymentRules
import id.diskola.app.viewmodel.SetPinViewModel

/**
 * "Atur Pin Baru" (doc `08` §4.2): two numeric password fields and "Simpan Perubahan". [fromLink] = the token came from the mailed
 * reset link, so the server is asked first whether it is still valid (§4.3).
 */
@Composable
fun SetPinScreen(
    token: String,
    fromLink: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetPinViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    LaunchedEffect(token, fromLink) { viewModel.validateLinkIfNeeded(token, fromLink) }
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.validationMessage) {
        state.validationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearValidationMessage()
        }
    }

    DetailScaffold(title = "Atur Pin Baru", onBack = onDone, modifier = modifier) { padding ->
        val linkError = state.linkError
        if (linkError != null) {
            ErrorState(
                modifier = Modifier.fillMaxSize().padding(padding),
                title = "Tautan tidak dapat dipakai",
                description = linkError,
                onRetry = onDone,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Text(
                    "Untuk keamanan akun, mohon jangan beritahukan pin Anda kepada siapapun selain pemilik akun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(PaymentRules.PIN_LENGTH) },
                    label = "Pin Baru",
                    placeholder = "Masukkan pin baru",
                    keyboardType = KeyboardType.NumberPassword,
                    visualTransformation = PasswordVisualTransformation(),
                )
                AppTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.filter(Char::isDigit).take(PaymentRules.PIN_LENGTH) },
                    label = "Ketik Ulang Pin Baru",
                    placeholder = "Ketik ulang pin baru",
                    keyboardType = KeyboardType.NumberPassword,
                    visualTransformation = PasswordVisualTransformation(),
                )
                AppButton(
                    text = "Simpan Perubahan",
                    onClick = { viewModel.save(token, pin, confirmation) },
                    enabled = PaymentRules.canSubmitNewPin(pin, confirmation),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.checking) AppLoadingDialog(message = "memproses permintaan Anda")
    if (state.saving) AppLoadingDialog(message = "menyimpan perubahan")
    if (state.saved) {
        AppDialog(
            onDismiss = onDone,
            dismissible = false,
            icon = Icons.Rounded.CheckCircle,
            title = "Pin Baru Berhasil Diatur",
            body = "Pin baru sudah berhasil di atur, hayo jangan lupa lagi ya",
            primaryButtonText = "Ok, Terima kasih",
            onPrimaryClick = onDone,
        )
    }
}
