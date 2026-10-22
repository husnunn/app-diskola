package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.KlaspayRepository
import id.diskola.app.utils.PaymentRules
import id.diskola.app.utils.PaymentRules.ResetPinPlan
import id.diskola.app.utils.PaymentRules.ResetPinResult
import id.diskola.app.utils.PinResultChannel
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

sealed interface ConfirmPinEvent {
    /** The e-mail is unverified or unknown: the e-mail dialogs have to run first. */
    data object NeedEmailSetting : ConfirmPinEvent

    /** The server trusts the e-mail: go straight to the new-PIN form. */
    data class OpenSetPin(val token: String) : ConfirmPinEvent
}

data class ConfirmPinState(
    val busy: String? = null,
    /** "Konfirmasi Pergantian Pin Terkirim": the reset link was mailed. */
    val mailSent: Boolean = false,
)

/**
 * `ConfirmPinPage` (doc `08` §2.3, §4.1): the PIN prompt in front of a payment, plus "Lupa pin?". The PIN itself only passes through
 * [PinResultChannel]; it is never kept in state, logged or sent anywhere from here.
 */
@HiltViewModel
class ConfirmPinViewModel @Inject constructor(
    private val klaspay: KlaspayRepository,
    private val sessionStore: SessionStore,
    private val pinResults: PinResultChannel,
) : BaseViewModel() {

    private val _state = MutableStateFlow(ConfirmPinState())
    val state: StateFlow<ConfirmPinState> = _state.asStateFlow()

    private val _events = Channel<ConfirmPinEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** The button: only a complete PIN is handed on. Returns whether it was. */
    fun confirm(requestKey: String, pin: String, channelName: String): Boolean {
        if (!PaymentRules.pinComplete(pin)) return false
        pinResults.deliver(requestKey, pin, channelName)
        return true
    }

    fun forgotPin() {
        if (_state.value.busy != null) return
        when (PaymentRules.resetPinPlan(sessionStore.isEmailVerified, sessionStore.user.email)) {
            ResetPinPlan.NeedEmailSetting -> viewModelScope.launch { _events.send(ConfirmPinEvent.NeedEmailSetting) }
            ResetPinPlan.SendReset -> sendReset()
        }
    }

    fun dismissMailSent() = _state.update { it.copy(mailSent = false) }

    private fun sendReset() {
        _state.update { it.copy(busy = "sedang memproses") }
        viewModelScope.launch {
            try {
                val outcome = klaspay.resetPin(sessionStore.user.email)
                _state.update { it.copy(busy = null) }
                when (PaymentRules.resetPinResult(outcome.verified, outcome.token)) {
                    ResetPinResult.OpenSetPin -> _events.send(ConfirmPinEvent.OpenSetPin(outcome.token))
                    ResetPinResult.ShowMailSent -> _state.update { it.copy(mailSent = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(busy = null) }
                handleError(e)
            }
        }
    }
}
