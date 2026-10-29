package id.diskola.app.dataclass.ResponData

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import id.diskola.app.di.module.NullToEmptyString
import id.diskola.app.di.module.NullToZeroInt

// Doc `08-keuangan-pembayaran-klaspay-ppob.md` §2.4–§2.7 and §9 (Bayar SPP). Money arrives as Int in legacy; here it is read as Double?
// (a number, a numeric string or null all read) and turned into whole rupiah with `PaymentRules.balanceOf`.

/** `GET payment/channel/spp` — category key (`virtual account`, `retail`, `saldo`, …) to its channels, in the server's order. */
@JsonClass(generateAdapter = true)
data class PaymentChannelResponse(
    val data: Map<String, List<PaymentChannelItem>>? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentChannelItem(
    @NullToZeroInt val channel_method_id: Int = 0,
    @NullToEmptyString val channel_method_name: String = "",
    @NullToEmptyString val channel_method_category: String = "",
    @NullToEmptyString val image_url: String = "",
    val is_active: Boolean? = null,
    @NullToZeroInt val channel_id: Int = 0,
)

/** `GET payment/transaction/history_school` — the student's SPP bills, paid and unpaid. */
@JsonClass(generateAdapter = true)
data class SppHistoryResponse(
    val data: SppHistoryData? = null,
)

@JsonClass(generateAdapter = true)
data class SppHistoryData(
    val transaction_invoice: List<SppInvoiceItem>? = null,
)

@JsonClass(generateAdapter = true)
data class SppInvoiceItem(
    @NullToEmptyString val transaction_id: String = "",
    val price: Double? = null,
    @NullToEmptyString val note: String = "",
    @NullToEmptyString val created_date: String = "",
    @NullToEmptyString val paid_date: String = "",
    val is_inquiry_channel: Boolean? = null,
)

/** `POST payment/transaction/spp_trx`: the bill ids joined with commas, the PIN, and the chosen channel (§9.5). */
@JsonClass(generateAdapter = true)
data class SppPayRequest(
    val transaction_id: String,
    val pin: String,
    val channel: Int,
    val channel_method_id: Int,
)

/** The answer of a payment (legacy `KlaspayBayarResponse`): for a non-wallet channel it carries the invoice to pay at the bank/shop. */
@JsonClass(generateAdapter = true)
data class PaymentTrxResponse(
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val message: String = "",
    val data: PaymentTrxData? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentTrxData(
    @NullToEmptyString val transaction_id: String = "",
    @NullToEmptyString val bank_name: String = "",
    @NullToEmptyString val retail_code: String = "",
    @NullToEmptyString val bank_image: String = "",
    @NullToEmptyString val note: String = "",
    val admin_fee: Double? = null,
    val amount: Double? = null,
    val total_amount: Double? = null,
    @NullToEmptyString val virtual_account: String = "",
    @NullToEmptyString val payment_code: String = "",
    @NullToEmptyString val partner_reff2: String = "",
    @NullToEmptyString val status: String = "",
    @NullToEmptyString val created_at: String = "",
    @NullToEmptyString val paid_at: String = "",
    @NullToEmptyString val expired: String = "",
)

/** `GET payment/channel/guide/{channel_method_name}` — guide type (ATM, Mobile Banking, …) to its numbered steps. */
@JsonClass(generateAdapter = true)
data class PaymentGuideResponse(
    val data: Map<String, List<String>>? = null,
)

/** `GET payment/transaction/history/id/{id}` (legacy `InqCheckResponse`), the fields the detail screen shows. */
@JsonClass(generateAdapter = true)
data class PaymentHistoryDetailResponse(
    val data: PaymentHistoryDetailData? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentHistoryDetailData(
    val transaction_detail: PaymentHistoryDetail? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentHistoryDetail(
    @NullToEmptyString val transaction_id: String = "",
    val amount: Double? = null,
    val total_amount: Double? = null,
    val fee_admin: Double? = null,
    @NullToEmptyString val type: String = "",
    @NullToEmptyString val transaction_status: String = "",
    @NullToEmptyString val created_at: String = "",
    @NullToEmptyString val note: String = "",
    @NullToEmptyString val paid_date: String = "",
    @NullToEmptyString val payment_code: String = "",
    @NullToEmptyString val payment_method: String = "",
    @NullToEmptyString val product_name: String = "",
)

/**
 * An invoice to pay outside the wallet (VA, minimarket, …), kept on the phone so the guide can be shown again (legacy `payment_invoice`).
 * Money is whole rupiah.
 */
@Entity(tableName = "payment_invoice")
data class PaymentInvoiceTable(
    @PrimaryKey val trx_id: String,
    val type: String = "",
    val product_name: String = "",
    val channel_method_name: String = "",
    val image_url: String = "",
    val payment_code: String = "",
    val partner_reff2: String = "",
    val admin_fee: Long = 0,
    val amount: Long = 0,
    val total_amount: Long = 0,
    val note: String = "",
    val created_at: String = "",
    val expired_at: String = "",
    val paid_at: String = "",
    val is_paid: Boolean = false,
    val cancelable: Boolean = false,
    val status: String = "",
)

/** The payment guide of one channel, cached for 24 hours. One row per channel, so a refresh replaces it (decision Q4). */
@Entity(tableName = "payment_guide")
data class PaymentGuideTable(
    @PrimaryKey val channel: String,
    /** The guide as the server sent it (type → steps), as JSON so the order of the sections is kept. */
    val sectionsJson: String,
    val updatedAt: Long,
)
