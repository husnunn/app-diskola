package id.diskola.app.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import id.diskola.app.apiservice.KlaspayApiService
import id.diskola.app.dataclass.ResponData.PaymentChannelItem
import id.diskola.app.dataclass.ResponData.PaymentGuideTable
import id.diskola.app.dataclass.ResponData.PaymentInvoiceTable
import id.diskola.app.dataclass.ResponData.SppPayRequest
import id.diskola.app.dataclass.localDb.PaymentDao
import id.diskola.app.utils.PaymentInvoiceRules
import id.diskola.app.utils.PaymentRules
import javax.inject.Inject

/** One SPP bill (`history_school` → legacy table `spp`): [name] is the month ("SPP Juli 2026"), [price] whole rupiah. */
data class SppBill(
    val transactionId: String,
    val name: String,
    val issuedAt: String,
    val paidAt: String,
    val price: Long,
    /** The bill is already waiting at a bank or shop: it has to be finished (or cancelled in Tagihanku) before paying again. */
    val inquiryChannel: Boolean,
)

/** A category of payment methods as the server ordered them (`virtual account`, `retail`, `saldo`, …). */
data class PaymentChannelGroup(val key: String, val channels: List<PaymentChannelItem>)

/** How a payment ended: paid straight from the wallet, or an invoice to finish at the bank/shop (shown by the guide). */
sealed interface PaymentOutcome {
    data object PaidFromWallet : PaymentOutcome
    data class InvoiceCreated(val trxId: String, val partnerReff2: String) : PaymentOutcome
}

/** The server accepted the request but said the payment did not go through; [message] is its text. */
class PaymentRejectedException(message: String) : Exception(message)

/**
 * Bayar SPP and the shared payment screens (doc `08` §2.4–§2.6, §9): bills, methods, `spp_trx`, the invoice kept for the guide, the guide
 * itself (cached a day, one row per channel) and the transaction detail.
 */
class SppPaymentRepository @Inject constructor(
    private val api: KlaspayApiService,
    private val paymentDao: PaymentDao,
    moshi: Moshi,
) {
    private val guideAdapter = moshi.adapter<Map<String, List<String>>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Types.newParameterizedType(List::class.java, String::class.java)),
    )

    suspend fun sppBills(): List<SppBill> =
        api.schoolTransactionHistory().data?.transaction_invoice.orEmpty().map {
            SppBill(
                transactionId = it.transaction_id,
                name = it.note,
                issuedAt = it.created_date,
                paidAt = it.paid_date,
                price = PaymentRules.balanceOf(it.price),
                inquiryChannel = it.is_inquiry_channel == true,
            )
        }

    suspend fun sppChannels(): List<PaymentChannelGroup> =
        api.sppChannels().data.orEmpty().map { (key, channels) -> PaymentChannelGroup(key, channels) }

    /**
     * `spp_trx`. From the wallet the bill is paid at once; any other channel answers with an invoice, kept on the phone for the guide
     * (`ConfirmPinViewModel.kt:75-118`). A reply whose status says it failed is a failure, not a success — legacy would have shown the
     * success page for it (the same rule the user set for transfers, Q8).
     */
    suspend fun paySpp(transactionIds: String, pin: String, channel: PaymentChannelItem, fromWallet: Boolean, nowIso: String): PaymentOutcome {
        val response = api.sppTransaction(SppPayRequest(transactionIds, pin, channel.channel_id, channel.channel_method_id))
        if (failedStatus(response.status)) throw PaymentRejectedException(response.message.ifBlank { "Pembayaran gagal" })
        if (fromWallet) return PaymentOutcome.PaidFromWallet
        val data = response.data?.takeIf { it.transaction_id.isNotBlank() }
            ?: throw PaymentRejectedException(response.message.ifBlank { "Data transaksi tidak ditemukan" })
        val invoice = PaymentInvoiceRules.fromPayment(data, type = "SPP", productName = "SPP", nowIso = nowIso)
        paymentDao.upsertInvoice(invoice)
        return PaymentOutcome.InvoiceCreated(invoice.trx_id, data.partner_reff2)
    }

    suspend fun invoice(trxId: String): PaymentInvoiceTable? = paymentDao.invoice(trxId)

    /**
     * The guide of [channel] (type → steps). A cached copy younger than a day is used as is; an older one is fetched again and replaced
     * (decision Q4 — legacy appended a second copy). When the network fails, an old copy is still better than nothing.
     */
    suspend fun guide(channel: String, now: Long): Map<String, List<String>> {
        val cached = paymentDao.guide(channel)
        if (cached != null && !PaymentInvoiceRules.isGuideStale(cached.updatedAt, now)) return decode(cached)
        return try {
            val sections = api.channelGuide(channel).data.orEmpty()
            paymentDao.upsertGuide(PaymentGuideTable(channel, guideAdapter.toJson(sections), now))
            sections
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null) decode(cached) else throw e
        }
    }

    /**
     * "Detail Pembayaran": the stored invoice when it has an amount, otherwise the server's detail merged over whatever was stored
     * (decision Q5 — amount, deadline, logo and cancelability are kept). Null when the server has no such transaction.
     */
    suspend fun detail(trxId: String): PaymentInvoiceTable? {
        val stored = paymentDao.invoice(trxId)
        if (PaymentInvoiceRules.usableForDetail(stored)) return stored
        val detail = api.transactionHistoryDetail(trxId).data?.transaction_detail ?: return null
        val merged = PaymentInvoiceRules.mergeDetail(stored, detail.copy(transaction_id = detail.transaction_id.ifBlank { trxId }))
        paymentDao.upsertInvoice(merged)
        return merged
    }

    private fun decode(row: PaymentGuideTable): Map<String, List<String>> = runCatching { guideAdapter.fromJson(row.sectionsJson) }.getOrNull().orEmpty()

    private fun failedStatus(status: String): Boolean {
        val s = status.lowercase()
        return "fail" in s || "error" in s || "reject" in s
    }
}
