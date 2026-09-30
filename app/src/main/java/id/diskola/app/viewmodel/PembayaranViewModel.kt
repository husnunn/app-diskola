package id.diskola.app.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.dataclass.mock.BillItem
import id.diskola.app.dataclass.mock.MockPembayaran
import id.diskola.app.dataclass.mock.TransferContact
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TopUpMethod { VIRTUAL_ACCOUNT, AGEN_KLASPAY }

/**
 * UI-only for now — Klaspay/Pembayaran has no backend yet (see `ref/fase2`). All balances,
 * bills, and transactions below are [MockPembayaran] seed data; swap the StateFlow sources for
 * real repository calls once the payment API exists.
 */
@HiltViewModel
class PembayaranViewModel @Inject constructor() : ViewModel() {

    private val _saldo = MutableStateFlow(MockPembayaran.SALDO)
    val saldo: StateFlow<Long> = _saldo.asStateFlow()

    val walletId = MockPembayaran.WALLET_ID
    val recentTransactions = MockPembayaran.recentTransactions
    val history = MockPembayaran.history
    val monthIncome = MockPembayaran.monthIncome
    val monthExpense = MockPembayaran.monthExpense
    val transferContacts = MockPembayaran.transferContacts
    val topUpPresets = MockPembayaran.topUpPresets

    private val _unpaidBills = MutableStateFlow(MockPembayaran.unpaidBills)
    val unpaidBills: StateFlow<List<BillItem>> = _unpaidBills.asStateFlow()

    val paidBills = MockPembayaran.paidBills

    private val _selectedBillIds = MutableStateFlow(
        MockPembayaran.unpaidBills.filter { it.overdue }.map { it.id }.toSet()
    )
    val selectedBillIds: StateFlow<Set<String>> = _selectedBillIds.asStateFlow()

    private val _topUpAmount = MutableStateFlow(0L)
    val topUpAmount: StateFlow<Long> = _topUpAmount.asStateFlow()

    private val _topUpMethod = MutableStateFlow(TopUpMethod.VIRTUAL_ACCOUNT)
    val topUpMethod: StateFlow<TopUpMethod> = _topUpMethod.asStateFlow()

    private val _selectedContact = MutableStateFlow<TransferContact?>(null)
    val selectedContact: StateFlow<TransferContact?> = _selectedContact.asStateFlow()

    private val _transferAmount = MutableStateFlow(0L)
    val transferAmount: StateFlow<Long> = _transferAmount.asStateFlow()

    fun toggleBillSelection(billId: String) {
        _selectedBillIds.value = _selectedBillIds.value.let {
            if (it.contains(billId)) it - billId else it + billId
        }
    }

    fun selectedBills(): List<BillItem> = _unpaidBills.value.filter { it.id in _selectedBillIds.value }

    fun setTopUpAmount(amount: Long) {
        _topUpAmount.value = amount
    }

    fun setTopUpMethod(method: TopUpMethod) {
        _topUpMethod.value = method
    }

    fun selectContact(contact: TransferContact) {
        _selectedContact.value = contact
    }

    fun setTransferAmount(amount: Long) {
        _transferAmount.value = amount
    }

    /** Called after a successful PIN confirmation — deducts the mock balance locally. */
    fun completePayment(amount: Long) {
        _saldo.value = (_saldo.value - amount).coerceAtLeast(0)
        _selectedBillIds.value = emptySet()
    }

    fun completeTopUp(amount: Long) {
        _saldo.value += amount
    }
}
