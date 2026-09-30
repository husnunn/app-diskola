package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.AuthRepository
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Doc `02-auth-login-sesi.md` §7 — only reachable from `PasswordScreen`, which is why `user_id`
 * (written by `check-account`) is already available in [SessionStore] here. */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    private val _sentEmail = MutableStateFlow("")
    val sentEmail: StateFlow<String> = _sentEmail.asStateFlow()

    private val _sent = MutableStateFlow(false)
    val sent: StateFlow<Boolean> = _sent.asStateFlow()

    fun sendResetLink(email: String) {
        launchWithHandling {
            authRepository.resetPassword(sessionStore.userId, email)
            _sentEmail.value = email
            _sent.value = true
        }
    }

    /** "kirim ulang" — same endpoint, doesn't reset [sent] so the sent screen stays put. Takes
     * `email` explicitly (from the route's own argument) rather than reading [sentEmail], since
     * the "sent" screen is a fresh nav back-stack entry with its own `ResetPasswordViewModel`
     * instance. */
    fun resend(email: String) {
        if (email.isBlank()) return
        launchWithHandling(showLoading = false) {
            authRepository.resetPassword(sessionStore.userId, email)
        }
    }
}
