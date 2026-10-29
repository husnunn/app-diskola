package id.diskola.app.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.apiservice.ApiException
import id.diskola.app.dataclass.ResponData.PaymentChannelItem
import id.diskola.app.repository.KlaspayRepository
import id.diskola.app.repository.PaymentChannelGroup
import id.diskola.app.repository.PaymentOutcome
import id.diskola.app.repository.SppBill
import id.diskola.app.repository.SppPaymentRepository
import id.diskola.app.utils.AppErrorHandler
import id.diskola.app.utils.PaymentChannelRules
import id.diskola.app.utils.PaymentRules
import id.diskola.app.utils.PinResultChannel
import id.diskola.app.utils.SppRules
import id.diskola.app.utils.session.SessionStore
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** The student card on top of Bayar SPP. */
data class SppHeader(val name: String, val nisn: String, val className: String, val balance: Long?)

data class SppBillsState(
    val unpaid: List<SppBill> = emptyList(),
    val paid: List<SppBill> = emptyList(),
    /** Indices into [unpaid]; always the oldest months first (see `SppRules.toggle`). */
    val selected: Set<Int> = emptySet(),
    val loading: Boolean = false,
    /** True once a load ended, so "Belum Ada Tagihan SPP" is not flashed before the first answer. */
    val loaded: Boolean = false,
) {
    val selectedBills: List<SppBill> get() = selected.sorted().mapNotNull { unpaid.getOrNull(it) }
    val total: Long get() = SppRules.total(unpaid.map { it.price }, selected)
}

data class PaymentMethodState(
    val groups: List<PaymentChannelGroup> = emptyList(),
    val loading: Boolean = false,
    /** The category opened like an accordion; one at a time. */
    val expanded: String? = null,
)

/** The dialogs of the SPP flow, all drawn with `AppDialog`. */
sealed interface SppDialog {
    /** A picked month is already waiting at a bank/shop (`is_inquiry_channel`). */
    data object PreviousBillPending : SppDialog

    /** "Peringatan": the amount does not suit the opened category (bank/retail). */
    data class AmountWarning(val message: String) : SppDialog

    /** The payment failed: the server's own words (decision Q16 — legacy always said "PIN Tidak Sesuai"). */
    data class PayFailed(val message: String) : SppDialog

    /** The method list could not be loaded; closing the dialog leaves the screen (legacy `alert(msg)` + finish). */
    data class MethodsFailed(val message: String) : SppDialog
}

sealed interface SppEvent {
    data class Toast(val text: String) : SppEvent
    data object OpenCheckout : SppEvent
    data object OpenTagihanku : SppEvent

    /** Ask for the PIN; the answer comes back through `PinResultChannel` under [PIN_REQUEST_KEY]. */
    data class AskPin(val channelName: String) : SppEvent
    data class PaidFromWallet(val amountLabel: String, val months: List<String>) : SppEvent
    data class InvoiceCreated(val trxId: String, val partnerReff2: String) : SppEvent
}

/**
 * Bayar SPP from the month list to the payment (doc `08` §9): one instance for the list, "Rincian Pembayaran" and "Pilih Metode
 * Pembayaran", scoped to the list's back-stack entry so the picked months travel along. The PIN never enters this state: it goes from
 * `PinResultChannel` straight into [pay].
 */
@HiltViewModel
class SppPaymentViewModel @Inject constructor(
    private val repository: SppPaymentRepository,
    private val klaspay: KlaspayRepository,
    private val sessionStore: SessionStore,
    private val pinResults: PinResultChannel,
) : BaseViewModel() {

    private val _header = MutableStateFlow(
        sessionStore.student.let { SppHeader(name = it?.name.orEmpty(), nisn = it?.nisn.orEmpty(), className = it?.className.orEmpty(), balance = null) },
    )
    val header: StateFlow<SppHeader> = _header.asStateFlow()

    private val _bills = MutableStateFlow(SppBillsState())
    val bills: StateFlow<SppBillsState> = _bills.asStateFlow()

    private val _methods = MutableStateFlow(PaymentMethodState())
    val methods: StateFlow<PaymentMethodState> = _methods.asStateFlow()

    private val _dialog = MutableStateFlow<SppDialog?>(null)
    val dialog: StateFlow<SppDialog?> = _dialog.asStateFlow()

    private val _paying = MutableStateFlow(false)
    val paying: StateFlow<Boolean> = _paying.asStateFlow()

    private val _events = MutableSharedFlow<SppEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SppEvent> = _events.asSharedFlow()

    /** The method picked before the PIN prompt; the payment needs it when the PIN comes back. */
    private var pendingChannel: PaymentChannelItem? = null
    private var pendingFromWallet = false
    private var billsInFlight = false

    // ---- Month list ----

    /** The list is shown again (first time, back from checkout or a payment): reload bills and balance and clear the picks, like legacy `onResume`. */
    fun onListResumed() {
        loadBills()
        loadBalance()
    }

    fun refresh() = onListResumed()

    private fun loadBills() {
        if (billsInFlight) return
        billsInFlight = true
        _bills.update { it.copy(loading = true, selected = emptySet()) }
        viewModelScope.launch {
            try {
                val all = repository.sppBills()
                val unpaid = SppRules.unpaidOldestFirst(all, { it.paidAt }, { it.issuedAt })
                val paid = SppRules.paid(all) { it.paidAt }
                _bills.update { it.copy(unpaid = unpaid, paid = paid, selected = emptySet()) }
                if (all.isEmpty()) toast("Data tidak ditemukan")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                toast(SppRules.loadError((e as? ApiException)?.responseCode, e.message))
            } finally {
                billsInFlight = false
                _bills.update { it.copy(loading = false, loaded = true) }
            }
        }
    }

    private fun loadBalance() {
        if (!sessionStore.klaspayActive) return
        viewModelScope.launch {
            try {
                val wallet = klaspay.wallet()
                _header.update { it.copy(balance = wallet?.balance ?: 0L) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _header.update { it.copy(balance = 0L) }
                toast(SppRules.walletError((e as? ApiException)?.responseCode, if (e is IOException) null else e.message))
            }
        }
    }

    fun toggle(index: Int, checked: Boolean) {
        _bills.update { it.copy(selected = SppRules.toggle(it.selected, index, checked)) }
    }

    /** "Bayar Sekarang" under the list. */
    fun onPayClicked() {
        val state = _bills.value
        if (state.selected.isEmpty()) return
        if (state.selectedBills.any { it.inquiryChannel }) {
            _dialog.value = SppDialog.PreviousBillPending
        } else {
            _events.tryEmit(SppEvent.OpenCheckout)
        }
    }

    fun openTagihanku() {
        _dialog.value = null
        _events.tryEmit(SppEvent.OpenTagihanku)
    }

    // ---- Payment methods ----

    /** The method screen opened: fresh channels and balance (the wallet row compares them). */
    fun loadMethods() {
        if (_methods.value.loading) return
        _methods.update { it.copy(loading = true) }
        loadBalance()
        viewModelScope.launch {
            try {
                _methods.update { it.copy(groups = repository.sppChannels()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                _dialog.value = SppDialog.MethodsFailed(AppErrorHandler.getMessage(e))
            } finally {
                _methods.update { it.copy(loading = false) }
            }
        }
    }

    /** Opening a category first checks the amount against it (`PaymentTypePage.kt:80-95`); a warning keeps it closed. */
    fun toggleCategory(key: String) {
        if (_methods.value.expanded == key) {
            _methods.update { it.copy(expanded = null) }
            return
        }
        val warning = PaymentChannelRules.amountWarning(key, _bills.value.total)
        if (warning != null) {
            _dialog.value = SppDialog.AmountWarning(warning)
        } else {
            _methods.update { it.copy(expanded = key) }
        }
    }

    /** A channel was picked: ask for the PIN. The wallet row only gets here when the balance covers the total. */
    fun choose(channel: PaymentChannelItem, fromWallet: Boolean) {
        if (_paying.value) return
        pendingChannel = channel
        pendingFromWallet = fromWallet
        _events.tryEmit(SppEvent.AskPin(channel.channel_method_name))
    }

    /** The method screen is visible again: when it is because the PIN prompt answered, pay with it. Backing out of the prompt leaves no answer. */
    fun onMethodScreenResumed() {
        pinResults.take(PIN_REQUEST_KEY)?.let { pay(it.pin) }
    }

    /** Pays (`spp_trx`) the picked months with the picked channel. */
    private fun pay(pin: String) {
        val channel = pendingChannel ?: return
        val state = _bills.value
        if (state.selected.isEmpty() || _paying.value) return
        val ids = SppRules.transactionIds(state.unpaid.map { it.transactionId }, state.selected)
        val amountLabel = PaymentRules.formatRupiah(state.total)
        val months = state.selectedBills.map { it.name }
        _paying.value = true
        viewModelScope.launch {
            try {
                when (val outcome = repository.paySpp(ids, pin, channel, pendingFromWallet, nowIso())) {
                    PaymentOutcome.PaidFromWallet -> _events.tryEmit(SppEvent.PaidFromWallet(amountLabel, months))
                    is PaymentOutcome.InvoiceCreated -> _events.tryEmit(SppEvent.InvoiceCreated(outcome.trxId, outcome.partnerReff2))
                }
                pendingChannel = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Bayar SPP gagal")
                _dialog.value = SppDialog.PayFailed(e.message?.takeIf { it.isNotBlank() && e !is IOException } ?: AppErrorHandler.getMessage(e))
            } finally {
                _paying.value = false
            }
        }
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    private fun toast(text: String) {
        _events.tryEmit(SppEvent.Toast(text))
    }

    private fun nowIso(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(Instant.now().atOffset(ZoneOffset.UTC))

    companion object {
        const val PIN_REQUEST_KEY = "spp_payment"
    }
}
