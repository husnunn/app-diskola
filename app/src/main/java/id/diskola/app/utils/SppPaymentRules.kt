package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.PaymentChannelItem
import id.diskola.app.dataclass.ResponData.PaymentHistoryDetail
import id.diskola.app.dataclass.ResponData.PaymentInvoiceTable
import id.diskola.app.dataclass.ResponData.PaymentTrxData
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Payment methods (`PaymentTypePage`, doc `08` §2.4): the category labels, the wallet row, the amount warnings. */
object PaymentChannelRules {

    const val SALDO_KEY = "saldo"

    /** The wallet's own channel inside the "saldo" category (`PaymentTypeViewmodel.kt:82-110`). */
    const val WALLET_CHANNEL_ID = 1

    const val WARNING_TITLE = "Peringatan"
    const val MIN_BANK_RETAIL = 10_000L
    const val MAX_RETAIL = 2_500_000L
    const val BELOW_MIN_MESSAGE = "Jumlah pembayaran harus lebih dari Rp 10,000 untuk menggunakan metode Bank atau Retail."
    const val ABOVE_RETAIL_MESSAGE = "Transaksi di atas Rp 2,500,000 tidak bisa menggunakan metode Retail. Gunakan metode Bank atau Saldo."
    const val SALDO_NOT_ENOUGH = "Saldo tidak cukup"

    enum class Icon { Online, Merchant, Atm, Saldo }

    data class Category(val key: String, val name: String, val info: String, val icon: Icon)

    /** The label of each category key (`PaymentTypeViewmodel.kt:127-154`); an unknown key (and "saldo") reads as "Saldo". */
    fun categoryOf(key: String): Category = when (key) {
        "e-money" -> Category(key, "E-Money", "Bayar menggunakan akun dompet", Icon.Online)
        "credit card" -> Category(key, "Kartu Kredit", "Bayar menggunakan kartu kredit", Icon.Online)
        "virtual account" -> Category(key, "Virtual Account", "Bayar cepat dengan akun virtual bank", Icon.Online)
        "clickpay" -> Category(key, "Click Pay", "Bayar cepat dengan aplikasi pembayaran", Icon.Online)
        "retail" -> Category(key, "Minimarket", "Bayar melalui minimarket", Icon.Merchant)
        "bank" -> Category(key, "Transfer Bank", "Bayar melalui transfer bank", Icon.Atm)
        "qris" -> Category(key, "QRIS", "Scan untuk membayar", Icon.Online)
        else -> Category(key, "Saldo", "Pembayaran lainnya", if (key == SALDO_KEY) Icon.Saldo else Icon.Online)
    }

    /** The channel the wallet pays with; null when the server listed none. */
    fun walletChannel(items: List<PaymentChannelItem>): PaymentChannelItem? = items.firstOrNull { it.channel_id == WALLET_CHANNEL_ID }

    fun needTopUp(balance: Long, amount: Long): Boolean = balance < amount

    /**
     * The "Peringatan" popup shown when a category is opened for an amount it cannot take (`PaymentTypePage.kt:80-95`). Only bank and
     * retail are checked; legacy's third branch could never be reached (a duplicated `< 10_000`) and is left out.
     */
    fun amountWarning(categoryKey: String, amount: Long): String? = when {
        amount < MIN_BANK_RETAIL && (categoryKey == "bank" || categoryKey == "retail") -> BELOW_MIN_MESSAGE
        amount > MAX_RETAIL && categoryKey == "retail" -> ABOVE_RETAIL_MESSAGE
        else -> null
    }
}

/** Bayar SPP (doc `08` §9): picking months in order, totals, the bill dialog, the error texts. */
object SppRules {

    const val INQUIRY_TITLE = "Selesaikan Tagihan SPP Sebelumnya"
    const val INQUIRY_MESSAGE = "Selesaikan pembayaran tagihan SPP yang sebelumnya. Kamu bisa cek di menu tagihanku"
    const val EMPTY_TITLE = "Belum Ada Tagihan SPP"
    const val EMPTY_UNPAID = "Segera lakukan pembayaran SPP sesuai tenggat waktu yang diberikan agar tidak terjadi tunggakan"
    const val EMPTY_PAID = "Lakukan pengecekan secara berkala untuk tagihan baru SPP Anda"

    /**
     * Months are paid oldest first (`SppTagihanPage.kt:220-250`): ticking month i ticks every month before it, unticking month i
     * unticks it and every month after it.
     */
    fun toggle(selected: Set<Int>, index: Int, checked: Boolean): Set<Int> =
        if (checked) (0..index).toSet() + selected
        else selected.filter { it < index }.toSet()

    fun total(prices: List<Long>, selected: Set<Int>): Long = selected.sumOf { prices.getOrElse(it) { 0L } }

    /** "Total Tagihan: (3x)". */
    fun totalLabel(count: Int): String = "Total Tagihan: (${count}x)"

    /** The ids sent to `spp_trx`, in the order of the list. */
    fun transactionIds(ids: List<String>, selected: Set<Int>): String = selected.sorted().mapNotNull { ids.getOrNull(it) }.joinToString(",")

    /** Unpaid bills, oldest first; legacy sorts the server's `created_date` as text (`SppDao.kt:21-22`). */
    fun <T> unpaidOldestFirst(items: List<T>, paidAt: (T) -> String, issuedAt: (T) -> String): List<T> =
        items.filter { paidAt(it).isBlank() }.sortedBy(issuedAt)

    fun <T> paid(items: List<T>, paidAt: (T) -> String): List<T> = items.filter { paidAt(it).isNotBlank() }

    /** The toast of a failed bill list (`SppTagihanPage.kt` error mapping), by HTTP code; [message] is the server's or the exception's. */
    fun loadError(code: Int?, message: String?): String = when (code) {
        403 -> "Akses Payment ditolak . Token tidak valid atau kadaluarsa."
        401 -> "Sesi kadaluarsa. Silakan login ulang."
        404 -> "Data tagihan tidak ditemukan"
        500 -> "Terjadi kesalahan pada server."
        null -> message?.takeIf { it.isNotBlank() } ?: "Terjadi kesalahan yang tidak diketahui"
        else -> "Gagal mengambil data: ${message.orEmpty()}"
    }

    /** The toast of a failed balance in the SPP header (`SppPaymentPage.kt:78-105`); the balance then reads 0. */
    fun walletError(code: Int?, message: String?): String = when (code) {
        403 -> "Akses wallet ditolak. Silakan login ulang."
        401 -> "Sesi kadaluarsa. Silakan login ulang."
        null -> "Gagal memuat saldo wallet. Periksa koneksi internet Anda."
        else -> "Gagal memuat saldo wallet: ${message.orEmpty()}"
    }
}

/** Status words and labels of the guide and detail screens (doc `08` §2.5, §2.6, §16.1). */
object PaymentStatusRules {

    enum class Tone { Success, Error, Warning, Info, Neutral }

    /** The chip on top of "Detail Pembayaran". "TANSAKSI DIBUAT" keeps legacy's typo, as the doc asks. */
    fun chip(status: String): Pair<String, Tone> {
        val s = status.lowercase()
        return when {
            "success" in s -> "PEMBAYARAN BERHASIL" to Tone.Success
            "new" in s -> "TANSAKSI DIBUAT" to Tone.Neutral
            "fail" in s || "reject" in s -> "PEMBAYARAN GAGAL" to Tone.Error
            else -> "MENUNGGU PEMBAYARAN" to Tone.Warning
        }
    }

    /** The colour of the raw "Status" row. */
    fun statusTone(status: String): Tone = when (status.lowercase()) {
        "success", "succes" -> Tone.Success
        "failed", "fail", "error" -> Tone.Error
        "pending", "wait", "waiting" -> Tone.Warning
        "new" -> Tone.Info
        else -> Tone.Neutral
    }

    /** "Nomor Virtual Account" for a bank channel, otherwise "Nomor Tagihan Pembayaran" (`PaymentViewModel.kt:117-123`). */
    fun numberLabel(channelMethodName: String): String =
        if (channelMethodName.contains("bank", ignoreCase = true)) "Nomor Virtual Account" else "Nomor Tagihan Pembayaran"

    fun isRetail(channelMethodName: String): Boolean = channelMethodName.contains("retail", ignoreCase = true)

    const val RETAIL_NOTE = "Pembayaran melalui alfamart dikenakan biaya dari alfamart sebesar Rp 2.500, dibayarkan ke kasir"

    fun isTopUp(type: String): Boolean = type.equals("topup", ignoreCase = true)

    fun detailCardTitle(type: String): String = if (isTopUp(type)) "Informasi Top-up" else "Informasi Pembayaran"

    fun amountRowLabel(type: String): String = if (isTopUp(type)) "Jumlah Top-up" else "Jumlah Pembayaran"

    /** "Gratis" (shown green) for no fee, otherwise the fee in rupiah. */
    fun adminFeeLabel(fee: Long): String = if (fee == 0L) "Gratis" else PaymentRules.formatRupiah(fee)

    fun deadlineNote(expiredAtRaw: String): String =
        "Segera transfer sebelum $expiredAtRaw agar tidak kadaluarsa. Perbedaan nilai transfer dapat menghambat proses transaksi pembayaran Anda."
}

/** Dates of the payment screens: the server's ISO UTC timestamps and the guide's deadline (doc `08` §2.5, §16.4). */
object PaymentDates {

    private val indonesian = Locale("id", "ID")
    private val isoUtc = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private val deadlinePatterns = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.US),
        DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", indonesian),
        DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale.US),
    )

    /** The guide's `expired_at`: the two local formats legacy tried, then ISO UTC; null when none fits. */
    fun parseDeadline(raw: String, zone: ZoneId): Instant? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        deadlinePatterns.forEach { format ->
            runCatching { return LocalDateTime.parse(text, format).atZone(zone).toInstant() }
        }
        return parseIsoUtc(text)
    }

    fun parseIsoUtc(raw: String): Instant? = runCatching { LocalDateTime.parse(raw.trim(), isoUtc).atZone(ZoneId.of("UTC")).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(raw.trim()) }.getOrNull()

    /** "26 Okt 2026" — the guide's deadline date. */
    fun guideDate(instant: Instant, zone: ZoneId): String = DateTimeFormatter.ofPattern("dd MMM yyyy", indonesian).format(instant.atZone(zone))

    /** "08:34:00" — the guide's deadline time. */
    fun guideTime(instant: Instant, zone: ZoneId): String = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US).format(instant.atZone(zone))

    /** "26 Oktober 2026, 08:34" from the server's ISO UTC; the raw text when it cannot be read. */
    fun display(raw: String, zone: ZoneId): String =
        parseIsoUtc(raw)?.let { DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", indonesian).format(it.atZone(zone)) } ?: raw
}

/** What is kept on the phone about an invoice, and when the guide is fetched again (doc `08` §2.5, §2.6, §9.5). */
object PaymentInvoiceRules {

    const val GUIDE_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000

    fun isGuideStale(updatedAt: Long, now: Long): Boolean = now - updatedAt > GUIDE_MAX_AGE_MILLIS

    /**
     * The invoice a non-wallet payment returns (`ConfirmPinViewModel.kt:75-118`): the bank's name or the shop code as the channel, the VA
     * or bill number as the code; no `paid_at` means "pending". [nowIso] fills a missing creation time.
     */
    fun fromPayment(data: PaymentTrxData, type: String, productName: String, nowIso: String): PaymentInvoiceTable = PaymentInvoiceTable(
        trx_id = data.transaction_id,
        type = type,
        product_name = productName,
        channel_method_name = data.bank_name.ifBlank { data.retail_code },
        image_url = data.bank_image,
        payment_code = data.virtual_account.ifBlank { data.payment_code },
        partner_reff2 = data.partner_reff2,
        admin_fee = PaymentRules.balanceOf(data.admin_fee),
        amount = PaymentRules.balanceOf(data.amount),
        total_amount = PaymentRules.balanceOf(data.total_amount),
        note = data.note,
        created_at = data.created_at.ifBlank { nowIso },
        expired_at = data.expired,
        paid_at = data.paid_at,
        is_paid = data.paid_at.isNotBlank(),
        cancelable = false,
        status = if (data.paid_at.isBlank()) "pending" else data.status,
    )

    /**
     * Fresh detail from the server over what the phone already had. Decision Q5: unlike legacy, `amount` stays the amount (not the total),
     * and the deadline, the logo, the partner reference and whether it can be cancelled are kept — the server's detail does not carry
     * them, and blanking them broke the guide afterwards.
     */
    fun mergeDetail(existing: PaymentInvoiceTable?, detail: PaymentHistoryDetail): PaymentInvoiceTable = PaymentInvoiceTable(
        trx_id = detail.transaction_id,
        type = detail.type.ifBlank { existing?.type.orEmpty() },
        product_name = detail.product_name.ifBlank { existing?.product_name.orEmpty() },
        channel_method_name = detail.payment_method.ifBlank { existing?.channel_method_name.orEmpty() },
        image_url = existing?.image_url.orEmpty(),
        payment_code = detail.payment_code.ifBlank { existing?.payment_code.orEmpty() },
        partner_reff2 = existing?.partner_reff2.orEmpty(),
        admin_fee = detail.fee_admin?.let(PaymentRules::balanceOf) ?: existing?.admin_fee ?: 0L,
        amount = detail.amount?.let(PaymentRules::balanceOf) ?: existing?.amount ?: 0L,
        total_amount = detail.total_amount?.let(PaymentRules::balanceOf) ?: existing?.total_amount ?: 0L,
        note = detail.note.ifBlank { existing?.note.orEmpty() },
        created_at = detail.created_at.ifBlank { existing?.created_at.orEmpty() },
        expired_at = existing?.expired_at.orEmpty(),
        paid_at = detail.paid_date,
        is_paid = detail.paid_date.isNotBlank(),
        cancelable = existing?.cancelable ?: false,
        status = detail.transaction_status.ifBlank { existing?.status.orEmpty() },
    )

    /** A stored invoice is shown as it is only when it has an amount (`PaymentViewModel.getDetailTransaction`). */
    fun usableForDetail(invoice: PaymentInvoiceTable?): Boolean = invoice != null && invoice.amount > 0
}
