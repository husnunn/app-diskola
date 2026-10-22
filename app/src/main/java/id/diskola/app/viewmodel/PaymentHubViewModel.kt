package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.KlaspayRepository
import id.diskola.app.repository.WalletInfo
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class PaymentHubUiState(
    /** The session says the wallet is activated (flag refreshed from every `payment/wallet` answer). */
    val walletActive: Boolean,
    /** Latest `payment/wallet`; null until it answered once. */
    val wallet: WalletInfo? = null,
    val isStudent: Boolean,
    val isTeacher: Boolean,
    val loading: Boolean = false,
    /** The wallet could not be read and nothing is known yet — the screen offers "Coba lagi". */
    val loadFailed: Boolean = false,
)

/**
 * Pembayaran tab (legacy `PembayaranPage`). Reads `payment/wallet` — only when the wallet is active — every time the tab comes back
 * and on pull-to-refresh, never overlapping itself. Legacy fetched once per fragment instance and again via `check-account` in
 * `onStart`; the latter already runs on the Pembelajaran hub, so it is not repeated here.
 */
@HiltViewModel
class PaymentHubViewModel @Inject constructor(
    private val klaspay: KlaspayRepository,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    private val _state = MutableStateFlow(
        PaymentHubUiState(
            walletActive = sessionStore.klaspayActive,
            isStudent = sessionStore.isStudent,
            isTeacher = sessionStore.isTeacher,
        ),
    )
    val state: StateFlow<PaymentHubUiState> = _state.asStateFlow()

    private var inFlight = false
    private var lastLoadedAt = 0L

    /** The tab is visible again (also after activation, which flips the session flag). */
    fun onResumed() = load(force = false)

    /** Pull-to-refresh and "Coba lagi". */
    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        val active = sessionStore.klaspayActive
        _state.update { it.copy(walletActive = active, isStudent = sessionStore.isStudent, isTeacher = sessionStore.isTeacher) }
        if (!active || inFlight) return
        if (!force && lastLoadedAt != 0L && System.currentTimeMillis() - lastLoadedAt < MIN_RELOAD_MS) return
        inFlight = true
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val wallet = klaspay.wallet()
                if (wallet != null) lastLoadedAt = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        walletActive = sessionStore.klaspayActive,
                        wallet = wallet ?: it.wallet,
                        loadFailed = wallet == null && it.wallet == null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                handleError(e)
                _state.update { it.copy(loadFailed = it.wallet == null) }
            } finally {
                inFlight = false
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private companion object {
        /** Only de-duplicates resume storms; the balance changes whenever a payment finishes, so it must stay short. */
        const val MIN_RELOAD_MS = 5_000L
    }
}
