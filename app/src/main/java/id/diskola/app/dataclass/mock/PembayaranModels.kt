package id.diskola.app.dataclass.mock

import java.util.Locale

private val idLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()

/** `"Rp " + n.toLocaleString('id-ID')` from the design — dot thousands-separator, no decimals. */
fun formatRupiah(amount: Long): String {
    return "Rp " + String.format(idLocale, "%,d", amount)
}

enum class TransactionDirection { IN, OUT }

data class TransactionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Long,
    val direction: TransactionDirection,
    val dateLabel: String,
)

data class BillItem(
    val id: String,
    val title: String,
    val amount: Long,
    val dueDateLabel: String,
    val overdue: Boolean,
    val paid: Boolean = false,
    val paidOnLabel: String? = null,
    val paidMethodLabel: String? = null,
)

data class TransferContact(
    val id: String,
    val name: String,
    val subtitle: String,
    val initials: String,
)

/** All mock data for the Klaspay/Pembayaran feature — no backend endpoint exists yet. */
object MockPembayaran {
    const val SALDO: Long = 1_250_000
    const val WALLET_ID = "7788 1234 0051"

    val recentTransactions = listOf(
        TransactionItem("t1", "SPP Juni 2026", "Pembayaran sekolah", 500_000, TransactionDirection.OUT, "1 Jun 2026"),
        TransactionItem("t2", "Top up · VA BNI", "Isi saldo", 1_000_000, TransactionDirection.IN, "28 Mei 2026"),
    )

    val history = listOf(
        TransactionItem("h1", "SPP Juni 2026", "Pembayaran sekolah", 500_000, TransactionDirection.OUT, "1 Jun 2026"),
        TransactionItem("h2", "Top up · VA BNI", "Isi saldo", 1_000_000, TransactionDirection.IN, "28 Mei 2026"),
        TransactionItem("h3", "Transfer ke Ahmad Fauzan", "Transfer sesama siswa", 50_000, TransactionDirection.OUT, "25 Mei 2026"),
        TransactionItem("h4", "Kantin Sekolah", "QR Pay", 25_000, TransactionDirection.OUT, "24 Mei 2026"),
        TransactionItem("h5", "Top up · Agen Klaspay", "Isi saldo", 200_000, TransactionDirection.IN, "20 Mei 2026"),
        TransactionItem("h6", "SPP Mei 2026", "Pembayaran sekolah", 500_000, TransactionDirection.OUT, "3 Mei 2026"),
    )

    val monthIncome = 1_250_000L
    val monthExpense = 643_000L

    val unpaidBills = listOf(
        BillItem("b1", "SPP Juli 2026", 500_000, "10 Jul 2026", overdue = true),
        BillItem("b2", "SPP Agustus 2026", 500_000, "10 Agu 2026", overdue = true),
        BillItem("b3", "SPP September 2026", 500_000, "10 Sep 2026", overdue = false),
        BillItem("b4", "Uang Kegiatan Semester 1", 250_000, "30 Sep 2026", overdue = false),
        BillItem("b5", "SPP Oktober 2026", 500_000, "10 Okt 2026", overdue = false),
    )

    val paidBills = listOf(
        BillItem("p1", "SPP Juni 2026", 500_000, "10 Jun 2026", overdue = false, paid = true, paidOnLabel = "3 Jun 2026", paidMethodLabel = "Saldo Klaspay"),
        BillItem("p2", "SPP Mei 2026", 500_000, "10 Mei 2026", overdue = false, paid = true, paidOnLabel = "3 Mei 2026", paidMethodLabel = "Saldo Klaspay"),
        BillItem("p3", "SPP April 2026", 500_000, "10 Apr 2026", overdue = false, paid = true, paidOnLabel = "2 Apr 2026", paidMethodLabel = "Virtual Account BNI"),
    )

    val transferContacts = listOf(
        TransferContact("c1", "Ahmad Fauzan", "Kelas 10 · Informatika", "AF"),
        TransferContact("c2", "Siti Nur Aisyah", "Kelas 11 · Informatika", "SA"),
        TransferContact("c3", "Kantin Sekolah", "Unit usaha · SMK DEMO", "KS"),
        TransferContact("c4", "Koperasi Siswa", "Unit usaha · SMK DEMO", "KO"),
    )

    val topUpPresets = listOf(50_000L, 100_000L, 200_000L, 500_000L, 1_000_000L, 2_000_000L)
    const val TOPUP_VA_FEE: Long = 1_500
    const val CHECKOUT_ADMIN_FEE: Long = 2_500
}
