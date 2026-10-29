package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.ResponData.PaymentInvoiceTable
import id.diskola.app.repository.SppPaymentRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class PaymentGuideState(
    val invoice: PaymentInvoiceTable? = null,
    /** Guide type (ATM, Mobile Banking, …) to its steps, in the server's order. */
    val sections: Map<String, List<String>> = emptyMap(),
    val loading: Boolean = true,
    val guideLoading: Boolean = false,
    /** The invoice is not on the phone: "Data transaksi tidak ditemukan" (`PaymentGuidePage.kt:154-163`). */
    val missing: Boolean = false,
)

/**
 * "Selesaikan Pembayaran" (doc `08` §2.5). The invoice comes only from the phone (it was saved when the payment was made); the guide is
 * fetched by channel name and cached a day.
 */
@HiltViewModel
class PaymentGuideViewModel @Inject constructor(
    private val repository: SppPaymentRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(PaymentGuideState())
    val state: StateFlow<PaymentGuideState> = _state.asStateFlow()

    private var started = false

    fun start(trxId: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            val invoice = repository.invoice(trxId)
            _state.update { it.copy(invoice = invoice, loading = false, missing = invoice == null) }
            if (invoice != null && invoice.channel_method_name.isNotBlank()) loadGuide(invoice.channel_method_name)
        }
    }

    private suspend fun loadGuide(channel: String) {
        _state.update { it.copy(guideLoading = true) }
        try {
            val sections = repository.guide(channel, System.currentTimeMillis())
            _state.update { it.copy(sections = sections) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The steps are a help, not the payment: without them the number and the total are still shown.
            Timber.e(e, "Petunjuk pembayaran gagal dimuat")
        } finally {
            _state.update { it.copy(guideLoading = false) }
        }
    }
}

data class PaymentDetailState(
    val invoice: PaymentInvoiceTable? = null,
    val loading: Boolean = true,
    val missing: Boolean = false,
)

/** "Detail Pembayaran" (doc `08` §2.6): the stored invoice, or the server's detail when the phone has none with an amount. */
@HiltViewModel
class PaymentDetailViewModel @Inject constructor(
    private val repository: SppPaymentRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(PaymentDetailState())
    val state: StateFlow<PaymentDetailState> = _state.asStateFlow()

    private var started = false

    fun start(trxId: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val invoice = repository.detail(trxId)
                _state.update { it.copy(invoice = invoice, loading = false, missing = invoice == null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(loading = false, missing = true) }
            }
        }
    }
}
