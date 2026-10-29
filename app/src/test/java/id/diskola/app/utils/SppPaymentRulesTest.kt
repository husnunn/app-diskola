package id.diskola.app.utils

import id.diskola.app.dataclass.ResponData.PaymentChannelItem
import id.diskola.app.dataclass.ResponData.PaymentHistoryDetail
import id.diskola.app.dataclass.ResponData.PaymentInvoiceTable
import id.diskola.app.dataclass.ResponData.PaymentTrxData
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SppPaymentRulesTest {

    private val jakarta = ZoneId.of("Asia/Jakarta")

    // ---- Channels ----

    @Test
    fun `category labels follow legacy, unknown keys read as Saldo`() {
        assertEquals("Virtual Account", PaymentChannelRules.categoryOf("virtual account").name)
        assertEquals("Minimarket", PaymentChannelRules.categoryOf("retail").name)
        assertEquals(PaymentChannelRules.Icon.Merchant, PaymentChannelRules.categoryOf("retail").icon)
        assertEquals("Transfer Bank", PaymentChannelRules.categoryOf("bank").name)
        assertEquals(PaymentChannelRules.Icon.Atm, PaymentChannelRules.categoryOf("bank").icon)
        assertEquals("Saldo", PaymentChannelRules.categoryOf("saldo").name)
        assertEquals(PaymentChannelRules.Icon.Saldo, PaymentChannelRules.categoryOf("saldo").icon)
        assertEquals("Saldo", PaymentChannelRules.categoryOf("something new").name)
        assertEquals("Pembayaran lainnya", PaymentChannelRules.categoryOf("something new").info)
    }

    @Test
    fun `the wallet channel is the one with channel id 1`() {
        val items = listOf(PaymentChannelItem(channel_method_id = 7, channel_id = 3), PaymentChannelItem(channel_method_id = 9, channel_id = 1))
        assertEquals(9, PaymentChannelRules.walletChannel(items)?.channel_method_id)
        assertNull(PaymentChannelRules.walletChannel(listOf(PaymentChannelItem(channel_id = 2))))
    }

    @Test
    fun `top up is needed only when the balance is below the amount`() {
        assertTrue(PaymentChannelRules.needTopUp(balance = 99_999, amount = 100_000))
        assertFalse(PaymentChannelRules.needTopUp(balance = 100_000, amount = 100_000))
    }

    @Test
    fun `bank and retail warn below 10_000, retail warns above 2_500_000, others never`() {
        assertEquals(PaymentChannelRules.BELOW_MIN_MESSAGE, PaymentChannelRules.amountWarning("bank", 9_999))
        assertEquals(PaymentChannelRules.BELOW_MIN_MESSAGE, PaymentChannelRules.amountWarning("retail", 5_000))
        assertNull(PaymentChannelRules.amountWarning("bank", 10_000))
        assertEquals(PaymentChannelRules.ABOVE_RETAIL_MESSAGE, PaymentChannelRules.amountWarning("retail", 2_500_001))
        assertNull(PaymentChannelRules.amountWarning("bank", 9_000_000))
        assertNull(PaymentChannelRules.amountWarning("virtual account", 1_000))
        assertNull(PaymentChannelRules.amountWarning("saldo", 1_000))
    }

    // ---- SPP ----

    @Test
    fun `ticking a month ticks every older month`() {
        assertEquals(setOf(0, 1, 2), SppRules.toggle(emptySet(), 2, checked = true))
        assertEquals(setOf(0, 1, 2, 3), SppRules.toggle(setOf(0, 1), 3, checked = true))
    }

    @Test
    fun `unticking a month unticks it and every newer month`() {
        assertEquals(setOf(0), SppRules.toggle(setOf(0, 1, 2, 3), 1, checked = false))
        assertEquals(emptySet<Int>(), SppRules.toggle(setOf(0, 1), 0, checked = false))
    }

    @Test
    fun `total, label and the ids sent to spp_trx`() {
        val prices = listOf(150_000L, 150_000L, 175_000L)
        assertEquals(300_000L, SppRules.total(prices, setOf(0, 1)))
        assertEquals("Total Tagihan: (2x)", SppRules.totalLabel(2))
        assertEquals("a,b,c", SppRules.transactionIds(listOf("a", "b", "c"), setOf(2, 0, 1)))
        assertEquals("a", SppRules.transactionIds(listOf("a", "b"), setOf(0, 9)))
    }

    @Test
    fun `unpaid bills oldest first, paid bills apart`() {
        data class Bill(val id: String, val issued: String, val paid: String)
        val bills = listOf(Bill("c", "2026-09-01", ""), Bill("a", "2026-07-01", ""), Bill("p", "2026-06-01", "2026-06-10"))
        assertEquals(listOf("a", "c"), SppRules.unpaidOldestFirst(bills, { it.paid }, { it.issued }).map { it.id })
        assertEquals(listOf("p"), SppRules.paid(bills) { it.paid }.map { it.id })
    }

    @Test
    fun `load and wallet errors use legacy texts per code`() {
        assertEquals("Akses Payment ditolak . Token tidak valid atau kadaluarsa.", SppRules.loadError(403, "x"))
        assertEquals("Data tagihan tidak ditemukan", SppRules.loadError(404, "x"))
        assertEquals("Gagal mengambil data: Bad Gateway", SppRules.loadError(502, "Bad Gateway"))
        assertEquals("Terjadi kesalahan yang tidak diketahui", SppRules.loadError(null, null))
        assertEquals("timeout", SppRules.loadError(null, "timeout"))
        assertEquals("Akses wallet ditolak. Silakan login ulang.", SppRules.walletError(403, null))
        assertEquals("Gagal memuat saldo wallet. Periksa koneksi internet Anda.", SppRules.walletError(null, "x"))
        assertEquals("Gagal memuat saldo wallet: oops", SppRules.walletError(500, "oops"))
    }

    // ---- Status & labels ----

    @Test
    fun `detail chip keeps legacy wording including the typo`() {
        assertEquals("PEMBAYARAN BERHASIL" to PaymentStatusRules.Tone.Success, PaymentStatusRules.chip("success"))
        assertEquals("TANSAKSI DIBUAT", PaymentStatusRules.chip("new").first)
        assertEquals("PEMBAYARAN GAGAL", PaymentStatusRules.chip("rejected").first)
        assertEquals("MENUNGGU PEMBAYARAN", PaymentStatusRules.chip("pending").first)
        assertEquals("MENUNGGU PEMBAYARAN", PaymentStatusRules.chip("").first)
    }

    @Test
    fun `raw status colour`() {
        assertEquals(PaymentStatusRules.Tone.Success, PaymentStatusRules.statusTone("Succes"))
        assertEquals(PaymentStatusRules.Tone.Error, PaymentStatusRules.statusTone("error"))
        assertEquals(PaymentStatusRules.Tone.Warning, PaymentStatusRules.statusTone("waiting"))
        assertEquals(PaymentStatusRules.Tone.Info, PaymentStatusRules.statusTone("new"))
        assertEquals(PaymentStatusRules.Tone.Neutral, PaymentStatusRules.statusTone("refund"))
    }

    @Test
    fun `labels of the guide and detail`() {
        assertEquals("Nomor Virtual Account", PaymentStatusRules.numberLabel("BANK BRI"))
        assertEquals("Nomor Tagihan Pembayaran", PaymentStatusRules.numberLabel("ALFAMART"))
        assertTrue(PaymentStatusRules.isRetail("RETAIL ALFAMART"))
        assertEquals("Informasi Top-up", PaymentStatusRules.detailCardTitle("TopUp"))
        assertEquals("Jumlah Pembayaran", PaymentStatusRules.amountRowLabel("SPP"))
        assertEquals("Gratis", PaymentStatusRules.adminFeeLabel(0))
        assertEquals("Rp 4.000", PaymentStatusRules.adminFeeLabel(4_000))
    }

    // ---- Dates ----

    @Test
    fun `deadline reads the local formats and ISO UTC, null otherwise`() {
        val local = PaymentDates.parseDeadline("26/10/2026 08:34", jakarta)
        assertEquals(Instant.parse("2026-10-26T01:34:00Z"), local)
        assertEquals(Instant.parse("2026-10-26T01:34:00Z"), PaymentDates.parseDeadline("26 Oktober 2026, 08:34", jakarta))
        assertEquals(Instant.parse("2026-10-26T01:34:00Z"), PaymentDates.parseDeadline("2026-10-26T01:34:00.000Z", jakarta))
        assertNull(PaymentDates.parseDeadline("besok", jakarta))
        assertNull(PaymentDates.parseDeadline("", jakarta))
    }

    @Test
    fun `display date is Indonesian in the phone's zone, raw text when unreadable`() {
        assertEquals("26 Oktober 2026, 08:34", PaymentDates.display("2026-10-26T01:34:00.000Z", jakarta))
        assertEquals("kemarin", PaymentDates.display("kemarin", jakarta))
        val deadline = Instant.parse("2026-10-26T01:34:05Z")
        assertEquals("08:34:05", PaymentDates.guideTime(deadline, jakarta))
        assertTrue(PaymentDates.guideDate(deadline, jakarta).startsWith("26 Okt"))
    }

    // ---- Invoice ----

    @Test
    fun `a non-wallet payment becomes a pending invoice with the VA as its code`() {
        val invoice = PaymentInvoiceRules.fromPayment(
            PaymentTrxData(
                transaction_id = "T1", bank_name = "BANK BRI", bank_image = "https://img/bri.png", virtual_account = "8888001",
                payment_code = "unused", admin_fee = 4000.0, amount = 150000.0, total_amount = 154000.0, expired = "27/10/2026 08:00",
                partner_reff2 = "R2",
            ),
            type = "SPP", productName = "SPP", nowIso = "2026-10-27T01:00:00.000Z",
        )
        assertEquals("BANK BRI", invoice.channel_method_name)
        assertEquals("8888001", invoice.payment_code)
        assertEquals(154_000L, invoice.total_amount)
        assertEquals(150_000L, invoice.amount)
        assertEquals("pending", invoice.status)
        assertEquals("2026-10-27T01:00:00.000Z", invoice.created_at)
        assertEquals("R2", invoice.partner_reff2)
        assertFalse(invoice.is_paid)
    }

    @Test
    fun `a retail payment uses the shop code and the bill number`() {
        val invoice = PaymentInvoiceRules.fromPayment(
            PaymentTrxData(transaction_id = "T2", retail_code = "ALFAMART", payment_code = "ALF123", paid_at = "2026-10-27T02:00:00.000Z", status = "success"),
            type = "SPP", productName = "SPP", nowIso = "x",
        )
        assertEquals("ALFAMART", invoice.channel_method_name)
        assertEquals("ALF123", invoice.payment_code)
        assertEquals("success", invoice.status)
        assertTrue(invoice.is_paid)
    }

    @Test
    fun `detail keeps amount, deadline, logo and cancelable of the stored invoice (Q5)`() {
        val stored = PaymentInvoiceTable(
            trx_id = "T1", amount = 150_000, total_amount = 154_000, expired_at = "27/10/2026 08:00", image_url = "logo",
            cancelable = true, partner_reff2 = "R2",
        )
        val merged = PaymentInvoiceRules.mergeDetail(
            stored,
            PaymentHistoryDetail(transaction_id = "T1", total_amount = 154000.0, transaction_status = "success", paid_date = "2026-10-27"),
        )
        assertEquals(150_000L, merged.amount)
        assertEquals(154_000L, merged.total_amount)
        assertEquals("27/10/2026 08:00", merged.expired_at)
        assertEquals("logo", merged.image_url)
        assertTrue(merged.cancelable)
        assertEquals("R2", merged.partner_reff2)
        assertEquals("success", merged.status)
        assertTrue(merged.is_paid)
    }

    @Test
    fun `detail without a stored invoice takes the server's amounts`() {
        val merged = PaymentInvoiceRules.mergeDetail(null, PaymentHistoryDetail(transaction_id = "T9", amount = 20000.0, total_amount = 22500.0, fee_admin = 2500.0))
        assertEquals(20_000L, merged.amount)
        assertEquals(22_500L, merged.total_amount)
        assertEquals(2_500L, merged.admin_fee)
        assertFalse(merged.cancelable)
    }

    @Test
    fun `guide is fetched again after 24 hours, and a stored invoice needs an amount`() {
        val day = PaymentInvoiceRules.GUIDE_MAX_AGE_MILLIS
        assertFalse(PaymentInvoiceRules.isGuideStale(updatedAt = 0, now = day))
        assertTrue(PaymentInvoiceRules.isGuideStale(updatedAt = 0, now = day + 1))
        assertFalse(PaymentInvoiceRules.usableForDetail(null))
        assertFalse(PaymentInvoiceRules.usableForDetail(PaymentInvoiceTable(trx_id = "x", amount = 0)))
        assertTrue(PaymentInvoiceRules.usableForDetail(PaymentInvoiceTable(trx_id = "x", amount = 1)))
    }
}
