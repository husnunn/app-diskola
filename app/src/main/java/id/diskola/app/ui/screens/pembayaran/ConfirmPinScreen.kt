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
import androidx.compose.material.icons.rounded.MarkEmailRead
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.components.AppButton
import id.diskola.app.ui.components.AppDialog
import id.diskola.app.ui.components.AppLoadingDialog
import id.diskola.app.ui.components.ButtonVariant
import id.diskola.app.ui.components.DetailScaffold
import id.diskola.app.ui.components.PinEntry
import id.diskola.app.ui.theme.ScreenHorizontalPadding
import id.diskola.app.ui.theme.Spacing
import id.diskola.app.utils.EmailApp
import id.diskola.app.utils.PaymentRules
import id.diskola.app.viewmodel.ConfirmPinEvent
import id.diskola.app.viewmodel.ConfirmPinViewModel

/**
 * `ConfirmPinPage` (doc `08` §2.3): "Masukkan pin", six dots + keypad, "Lupa pin?" and the confirm button — "Konfirmasi" when only
 * the PIN is wanted, "Bayar Sekarang" when it authorises a payment ([forPayment]). The PIN goes back to the asking screen through
 * `PinResultChannel` under [requestKey] and is not kept here.
 */
@Composable
fun ConfirmPinScreen(
    requestKey: String,
    channelName: String,
    forPayment: Boolean,
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    onNeedEmailSetting: () -> Unit,
    onOpenSetPin: (token: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfirmPinViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ConfirmPinEvent.NeedEmailSetting -> onNeedEmailSetting()
                is ConfirmPinEvent.OpenSetPin -> onOpenSetPin(event.token)
            }
        }
    }

    DetailScaffold(title = "Konfirmasi Pembayaran", onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text("Masukkan pin", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            PinEntry(pin = pin, onPinChange = { pin = it })
            AppButton(text = "Lupa pin?", onClick = viewModel::forgotPin, variant = ButtonVariant.Text)
            AppButton(
                text = if (forPayment) "Bayar Sekarang" else "Konfirmasi",
                onClick = { if (viewModel.confirm(requestKey, pin, channelName)) onConfirmed() },
                enabled = PaymentRules.pinComplete(pin),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.busy?.let { AppLoadingDialog(message = it) }
    if (state.mailSent) {
        AppDialog(
            onDismiss = viewModel::dismissMailSent,
            icon = Icons.Rounded.MarkEmailRead,
            title = "Konfirmasi Pergantian Pin Terkirim",
            body = "Konfirmasi pergantian pin telah kami kirim ke email kamu yang terdaftar di Diskola. Periksa email tersebut untuk mendapat pin baru",
            primaryButtonText = "Ok, Terima Kasih",
            onPrimaryClick = {
                viewModel.dismissMailSent()
                EmailApp.open(context)
            },
        )
    }
}
