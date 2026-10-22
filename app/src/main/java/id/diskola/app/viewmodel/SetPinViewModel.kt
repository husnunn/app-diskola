package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.KlaspayRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PaymentRules
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class SetPinState(
    /** The mailed link is being checked with the server (only when the screen was opened from a link). */
    val checking: Boolean = false,
    /** The link is no longer valid: the form is replaced by this message. */
    val linkError: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    /** Validation text for the last "Simpan Perubahan" (legacy showed it as a toast). */
    val validationMessage: String? = null,
)

/** `SetPinKlaspayPage` (doc `08` §4.2, §4.3): the new-PIN form behind a reset token. */
@HiltViewModel
class SetPinViewModel @Inject constructor(
    private val klaspay: KlaspayRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(SetPinState())
    val state: StateFlow<SetPinState> = _state.asStateFlow()

    private var validated = false

    /** Once per screen: a token that came from a mailed link is checked first (legacy `DeepLinkPage`). */
    fun validateLinkIfNeeded(token: String, fromLink: Boolean) {
        if (!fromLink || validated) return
        validated = true
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            try {
                klaspay.validateResetToken(token)
                _state.update { it.copy(checking = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(checking = false, linkError = AppErrorHandler.getMessage(e)) }
            }
        }
    }

    fun save(token: String, pin: String, confirmation: String) {
        if (_state.value.saving) return
        val problem = PaymentRules.validateNewPin(pin, confirmation)
        if (problem != null) {
            _state.update { it.copy(validationMessage = problem) }
            return
        }
        _state.update { it.copy(saving = true, validationMessage = null) }
        viewModelScope.launch {
            try {
                klaspay.setNewPin(pin, token)
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(saving = false) }
                handleError(e)
            }
        }
    }

    fun clearValidationMessage() = _state.update { it.copy(validationMessage = null) }
}
