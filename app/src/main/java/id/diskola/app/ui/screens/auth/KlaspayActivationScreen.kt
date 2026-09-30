package id.diskola.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.NumericKeypad
import id.diskola.app.ui.components.PinDots
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.viewmodel.KlaspayActivationViewModel

private const val PIN_LENGTH = 6

/**
 * `KlaspayAktivasiPage`'s actually-reachable path — doc `08-keuangan-pembayaran-klaspay-ppob.md`
 * §3: create-PIN → confirm-PIN, the password step is dead code in the legacy nav graph and isn't
 * ported. Only the SSO entry point (`isSso=true`) reaches this in the current auth scope.
 */
@Composable
fun KlaspayActivationScreen(
    isSso: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KlaspayActivationViewModel = hiltViewModel(),
) {
    var showIntroAlert by remember { mutableStateOf(isSso) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var mismatch by remember { mutableStateOf(false) }

    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val activated by viewModel.activated.collectAsStateWithLifecycle()

    LaunchedEffect(activated) {
        if (activated) onDone()
    }

    fun onDigit(digit: Char) {
        if (!confirming) {
            if (pin.length < PIN_LENGTH) pin += digit
            if (pin.length == PIN_LENGTH) confirming = true
        } else {
            if (confirmPin.length < PIN_LENGTH) confirmPin += digit
            if (confirmPin.length == PIN_LENGTH) {
                if (confirmPin == pin) {
                    viewModel.activate(pin, isSso)
                } else {
                    mismatch = true
                    confirmPin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (!confirming) {
            pin = pin.dropLast(1)
        } else {
            confirmPin = confirmPin.dropLast(1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (confirming) "KONFIRMASI PIN ULANG" else "BUAT 6 DIGIT PIN KEAMANAN",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.xl))
        PinDots(length = if (confirming) confirmPin.length else pin.length, total = PIN_LENGTH)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.xxl))
        NumericKeypad(onDigit = ::onDigit, onBackspace = ::onBackspace)

        if (mismatch) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.md))
            Text("PIN tidak sama, coba lagi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (errorMessage.isNotBlank()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(Spacing.md))
            Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showIntroAlert) {
        AppDialog(
            onDismiss = { showIntroAlert = false },
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "Aktivasi Pin Wallet",
            body = "Proses aktivasi wallet dibutuhkan untuk keperluan transaksi anda selama di aplikasi",
            primaryButtonText = "SIAP",
            onPrimaryClick = { showIntroAlert = false },
            dismissible = false,
        )
    }

    if (loading) {
        id.diskola.app.ui.components.AppLoadingDialog(message = "mohon tunggu")
    }
}
