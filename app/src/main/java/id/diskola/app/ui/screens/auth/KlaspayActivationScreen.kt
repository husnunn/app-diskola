package id.diskola.app.ui.screens.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.PinEntry
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.PaymentRules
import id.diskola.app.viewmodel.KlaspayActivationViewModel

/**
 * `KlaspayAktivasiPage`'s reachable path — doc `08-keuangan-pembayaran-klaspay-ppob.md` §3: create the PIN, confirm it, then
 * `payment/activate` (the password step is dead code in the legacy nav graph and is not ported). "KONFIRMASI" is enabled only for six
 * digits on the first step and for an identical PIN on the second; back on the second step returns to the first, on the first it
 * leaves the screen ([onBack]).
 *
 * After success the dialog offers "TOP UP SEKARANG" and "NANTI SAJA" (both end with [onDone]). While Top Up is not built
 * ([onTopUp] null) the first one explains that and carries on like "NANTI SAJA".
 */
@Composable
fun KlaspayActivationScreen(
    isSso: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onTopUp: (() -> Unit)? = null,
    viewModel: KlaspayActivationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var showIntroAlert by remember { mutableStateOf(isSso) }
    var topUpUnavailable by remember { mutableStateOf(false) }
    // PINs stay out of saveable state: they would otherwise be written into the saved-instance bundle.
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }

    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val activated by viewModel.activated.collectAsStateWithLifecycle()

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    fun goBack() {
        if (confirming) {
            confirming = false
            confirmation = ""
        } else {
            onBack()
        }
    }
    BackHandler(enabled = !activated) { goBack() }

    DetailScaffold(title = "Buat PIN Wallet", onBack = ::goBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                if (confirming) "KONFIRMASI PIN ULANG" else "BUAT 6 DIGIT PIN KEAMANAN",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                if (confirming) "Ketik ulang  pin keamanan yang sudah Anda atur sebelumnya"
                else "Akan diminta setiap Anda akan melakukan transaksi pembayaran",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (confirming) {
                PinEntry(
                    pin = confirmation,
                    onPinChange = { confirmation = it },
                    errorText = if (PaymentRules.showPinMismatch(pin, confirmation)) "Pin yang Anda masukkan tidak sama" else null,
                )
            } else {
                PinEntry(pin = pin, onPinChange = { pin = it })
            }
            AppButton(
                text = "KONFIRMASI",
                onClick = { if (confirming) viewModel.activate(pin, isSso) else confirming = true },
                enabled = if (confirming) PaymentRules.pinMatches(pin, confirmation) else PaymentRules.pinComplete(pin),
                modifier = Modifier.fillMaxWidth(),
            )
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
    if (loading) AppLoadingDialog(message = "mohon tunggu")

    if (activated) {
        AppDialog(
            onDismiss = {},
            dismissible = false,
            icon = Icons.Rounded.CheckCircle,
            title = "Aktivasi Wallet Berhasil!",
            body = "Asyiik, sekarang kamu bisa nikmati kemudahan pembayaran apa saja cukup pakai Wallet",
            primaryButtonText = "TOP UP SEKARANG",
            onPrimaryClick = { if (onTopUp != null) onTopUp() else topUpUnavailable = true },
            secondaryButtonText = "NANTI SAJA",
            onSecondaryClick = onDone,
        )
    }
    if (topUpUnavailable) {
        AppDialog(
            onDismiss = {},
            dismissible = false,
            title = "Fitur dalam pengembangan",
            body = "Menu ini sedang dalam proses pembangunan, ditunggu updatenya yah...",
            primaryButtonText = "Ok Deh",
            onPrimaryClick = onDone,
        )
    }
}
