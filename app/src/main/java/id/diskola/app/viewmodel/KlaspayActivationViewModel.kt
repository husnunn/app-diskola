package id.diskola.app.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.repository.KlaspayRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Doc `08-keuangan-pembayaran-klaspay-ppob.md` §3 — only the 2-step PIN flow that's actually
 * reachable (`KlaspayAktivasiPin` → `KlaspayAktivasiPinConfirm`); the password step is dead code in
 * the legacy nav graph, so it's not ported here either. */
@HiltViewModel
class KlaspayActivationViewModel @Inject constructor(
    private val klaspay: KlaspayRepository,
) : BaseViewModel() {

    private val _activated = MutableStateFlow(false)
    val activated: StateFlow<Boolean> = _activated.asStateFlow()

    /** `payment/activate` with the confirmed PIN; a failure leaves the user on the confirmation step with the reason as a toast. */
    fun activate(pin: String, isSso: Boolean) {
        if (loading.value) return
        launchWithHandling {
            klaspay.activate(pin, fromSso = isSso)
            _activated.value = true
        }
    }
}
