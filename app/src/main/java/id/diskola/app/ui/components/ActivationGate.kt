package id.diskola.app.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.diskola.app.ui.screens.auth.clearGoogleCredentialState
import id.diskola.app.ui.screens.auth.signInWithGoogle
import id.diskola.app.viewmodel.ActivationGateEvent
import id.diskola.app.viewmodel.ActivationGateViewModel
import id.diskola.app.viewmodel.GateDialog

/** What a screen can ask of the activation gate. */
class ActivationGate internal constructor(
    /** A wallet-only item was tapped while the wallet is inactive. */
    val prompt: () -> Unit,
    /** "Aktivasi Wallet" / "Aktivasi Sekarang" / the Home bell: run every check, then open PIN creation. */
    val start: () -> Unit,
    /** "Lupa pin?" without a verified e-mail. */
    val startEmailSetting: () -> Unit,
)

/**
 * Hosts the dialogs of [ActivationGateViewModel] at the place it is called and returns the handle that starts them. Call it once
 * per shell: the Pembayaran hub and the Home bell share the result. [onOpenActivation] runs once every check passed.
 */
@Composable
fun rememberActivationGate(
    onOpenActivation: () -> Unit,
    viewModel: ActivationGateViewModel = hiltViewModel(),
): ActivationGate {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val latestOpen by rememberUpdatedState(onOpenActivation)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ActivationGateEvent.OpenActivation -> latestOpen()
                is ActivationGateEvent.Message -> Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    state.busy?.let { AppLoadingDialog(message = it) }

    when (state.dialog) {
        GateDialog.ActivateFirst -> ActivateWalletFirstDialog(onActivate = viewModel::start, onLater = viewModel::dismiss)
        GateDialog.EmailSetting -> EmailSettingDialog(
            initialEmail = state.email,
            onPickEmail = {
                clearGoogleCredentialState(context)
                val picked = signInWithGoogle(context)?.email?.takeIf { it.isNotBlank() }
                if (picked == null) Toast.makeText(context, "Gagal mengambil email dari Google", Toast.LENGTH_SHORT).show()
                picked
            },
            onVerify = viewModel::submitEmail,
            onLater = viewModel::finishEmailStep,
        )
        GateDialog.EmailConfirm -> EmailConfirmDialog(onOk = viewModel::finishEmailStep)
        GateDialog.ChangePassword -> ChangePasswordDialog(
            cancelable = state.passwordCancelable,
            onSubmit = viewModel::changePassword,
            onInvalid = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
            onLater = viewModel::dismiss,
        )
        GateDialog.PasswordChanged -> PasswordChangedDialog(onOk = viewModel::passwordChangedAcknowledged)
        null -> Unit
    }

    state.emailUsedMessage?.let { EmailUsedDialog(message = it, onClose = viewModel::dismissEmailUsed) }

    return remember(viewModel) {
        ActivationGate(
            prompt = viewModel::promptActivation,
            start = viewModel::start,
            startEmailSetting = viewModel::startEmailSetting,
        )
    }
}
