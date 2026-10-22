package id.diskola.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.AccountSecurityRepository
import id.diskola.app.repository.KlaspayRepository
import id.diskola.app.repository.PaymentCheckResult
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PaymentRules
import id.diskola.app.utils.PaymentRules.ActivationStep
import id.diskola.app.utils.SecurityDialogRules
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** The dialogs standing in front of wallet activation (doc `08` §5.3 and `02` §8.2). */
enum class GateDialog { ActivateFirst, EmailSetting, EmailConfirm, ChangePassword, PasswordChanged }

/** Why the dialogs were opened: activation continues with the next check afterwards, "Lupa pin?" stops after the e-mail step. */
enum class GateContext { Activation, ForgotPin }

data class ActivationGateState(
    val dialog: GateDialog? = null,
    val context: GateContext = GateContext.Activation,
    /** A blocking progress message ("Memverifikasi data"…), or null. */
    val busy: String? = null,
    /** The "PEMBERITAHUAN / Email sudah digunakan oleh akun:" dialog, when `change-profile` or the mail failed. */
    val emailUsedMessage: String? = null,
    /** The account's current e-mail, shown in the e-mail dialog. */
    val email: String = "",
    val passwordCancelable: Boolean = true,
)

sealed interface ActivationGateEvent {
    /** Every check passed: go to the PIN creation screen. */
    data object OpenActivation : ActivationGateEvent

    data class Message(val text: String) : ActivationGateEvent
}

/**
 * One state machine for everything in front of wallet activation: the "activate first" prompt, the e-mail dialogs, the default-
 * password dialog and the server's `payment/check`. Hosted once in the main shell so the Pembayaran hub and the Home bell share it.
 * The step order is [PaymentRules]; this class only performs the calls and keeps the dialog state.
 */
@HiltViewModel
class ActivationGateViewModel @Inject constructor(
    private val klaspay: KlaspayRepository,
    private val security: AccountSecurityRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ActivationGateState())
    val state: StateFlow<ActivationGateState> = _state.asStateFlow()

    private val _events = Channel<ActivationGateEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ---- Entry points ----

    /** A wallet-only item was tapped while the wallet is inactive: "Aktivasi Wallet Lebih Dahulu". */
    fun promptActivation() = open(GateDialog.ActivateFirst, GateContext.Activation)

    /** "Aktivasi Wallet" / "Aktivasi Sekarang": local checks, then the server check. */
    fun start() {
        if (_state.value.busy != null) return
        _state.update { it.copy(context = GateContext.Activation) }
        handle(PaymentRules.activationPreflight(sessionStore.isEmailVerified, sessionStore.defaultPass))
    }

    /** "Lupa pin?" with an unverified e-mail: just the e-mail dialogs. */
    fun startEmailSetting() = open(GateDialog.EmailSetting, GateContext.ForgotPin)

    // ---- Dialog results ----

    fun dismiss() = _state.update { it.copy(dialog = null) }

    fun dismissEmailUsed() = _state.update { it.copy(emailUsedMessage = null) }

    /** "Nanti Saja" or back on the e-mail dialog, and "Ok Saya Konfirmasi Dulu" on the confirmation. */
    fun finishEmailStep() {
        if (_state.value.context == GateContext.ForgotPin) {
            dismiss()
        } else {
            handle(PaymentRules.afterEmailStep(sessionStore.defaultPass))
        }
    }

    /** "Verifikasi Sekarang": update the e-mail, then send the verification mail. */
    fun submitEmail(email: String) {
        if (_state.value.busy != null || !SecurityDialogRules.canSendVerification(email)) return
        _state.update { it.copy(busy = "Mengirimkan verifikasi...") }
        viewModelScope.launch {
            try {
                security.changeEmail(email)
                security.requestEmailVerification()
                _state.update { it.copy(busy = null, dialog = GateDialog.EmailConfirm, email = email) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(busy = null, emailUsedMessage = AppErrorHandler.getMessage(e)) }
            }
        }
    }

    /** "Buat Password Baru" after the user confirmed ("Saya Yakin"). */
    fun changePassword(old: String, new: String) {
        if (_state.value.busy != null) return
        _state.update { it.copy(busy = "mohon tunggu") }
        viewModelScope.launch {
            try {
                security.changePassword(old, new)
                _state.update { it.copy(busy = null, dialog = GateDialog.PasswordChanged) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(busy = null) }
                _events.send(ActivationGateEvent.Message(AppErrorHandler.getMessage(e)))
            }
        }
    }

    /** "Ok, Kembali" after the password changed: the checks start over from the top. */
    fun passwordChangedAcknowledged() {
        if (_state.value.context == GateContext.ForgotPin) {
            dismiss()
        } else {
            handle(PaymentRules.afterPasswordChanged(sessionStore.isEmailVerified))
        }
    }

    // ---- Internals ----

    private fun handle(step: ActivationStep) {
        when (step) {
            ActivationStep.NeedEmail -> open(GateDialog.EmailSetting, _state.value.context)
            ActivationStep.NeedPassword -> open(GateDialog.ChangePassword, _state.value.context)
            ActivationStep.CallCheck -> runCheck()
            ActivationStep.Stop -> dismiss()
        }
    }

    private fun open(dialog: GateDialog, context: GateContext) {
        _state.update {
            it.copy(
                dialog = dialog,
                context = context,
                email = sessionStore.user.email,
                passwordCancelable = SecurityDialogRules.passwordDialogCancelable(sessionStore.klaspayActive),
            )
        }
    }

    private fun runCheck() {
        _state.update { it.copy(dialog = null, busy = "Memverifikasi data") }
        viewModelScope.launch {
            try {
                when (val result = klaspay.preflightCheck()) {
                    PaymentCheckResult.Allowed -> {
                        _state.update { it.copy(busy = null) }
                        _events.send(ActivationGateEvent.OpenActivation)
                    }
                    is PaymentCheckResult.Rejected -> {
                        _state.update { it.copy(busy = null) }
                        val step = PaymentRules.stepForCheckError(result.errorTypes.toTypedArray())
                        // Legacy stayed silent on any other rejection; the reason is shown here.
                        if (step == ActivationStep.Stop && result.message.isNotBlank()) {
                            _events.send(ActivationGateEvent.Message(result.message))
                        }
                        handle(step)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(busy = null) }
                _events.send(ActivationGateEvent.Message(AppErrorHandler.getMessage(e)))
            }
        }
    }
}
