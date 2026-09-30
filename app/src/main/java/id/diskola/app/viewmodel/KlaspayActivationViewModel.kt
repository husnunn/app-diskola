package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.apiservice.ActivateKlaspayRequest
import id.diskola.app.apiservice.KlaspayApiService
import id.diskola.app.utils.session.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Doc `08-keuangan-pembayaran-klaspay-ppob.md` §3 — only the 2-step PIN flow that's actually
 * reachable (`KlaspayAktivasiPin` → `KlaspayAktivasiPinConfirm`); the password step is dead code in
 * the legacy nav graph, so it's not ported here either. */
@HiltViewModel
class KlaspayActivationViewModel @Inject constructor(
    private val klaspayApiService: KlaspayApiService,
    private val sessionStore: SessionStore,
) : BaseViewModel() {

    private val _activated = MutableStateFlow(false)
    val activated: StateFlow<Boolean> = _activated.asStateFlow()

    fun activate(pin: String, isSso: Boolean) {
        launchWithHandling {
            klaspayApiService.activateKlaspay(ActivateKlaspayRequest(pin = pin))
            sessionStore.markKlaspayActivated(fromSso = isSso)
            _activated.value = true
        }
    }
}
