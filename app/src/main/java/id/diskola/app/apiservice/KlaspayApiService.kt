package id.diskola.app.apiservice

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** `POST mobile/app/payment/activate` — doc `08-keuangan-pembayaran-klaspay-ppob.md` §3. The
 * legacy app's own nav graph skips the password step (`klaspay_aktivasi_nav.xml` start destination
 * is the PIN step), so `password` is always sent empty — that's not a bug this app introduced. */
@JsonClass(generateAdapter = true)
data class ActivateKlaspayRequest(val pin: String, val password: String = "")

@JsonClass(generateAdapter = true)
data class ActivateKlaspayApiResponse(val data: Map<String, Any>? = null)

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface KlaspayApiService {

    // --- Aktivasi & wallet ---
    @POST("mobile/app/payment/check")
    suspend fun checkPaymentStatus(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/payment/activate")
    suspend fun activateKlaspay(@Body request: ActivateKlaspayRequest): ActivateKlaspayApiResponse

    @GET("payment/wallet")
    suspend fun getWallet(): Map<String, Any>

    @POST("payment/toppers/register")
    suspend fun registerToppers(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/toppers/unregister")
    suspend fun unregisterToppers(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/reset-pin")
    suspend fun resetPin(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/reset-pin/setpin")
    suspend fun setNewPin(@Body request: Map<String, Any>): Map<String, Any>

    @GET("payment/user/campaign/point")
    suspend fun campaignPoints(): Map<String, Any>

    @GET("payment/merchant/id/{merchantId}")
    suspend fun merchantInfo(@Path("merchantId") merchantId: String): Map<String, Any>

    // --- Topup & transfer ---
    @GET("payment/channel/topup")
    suspend fun topupChannels(): Map<String, Any>

    @POST("payment/transaction/topup_inq")
    suspend fun topupInquiry(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/transaction/topup_trx")
    suspend fun topupTransaction(@Body request: Map<String, Any>): Map<String, Any>

    @GET("payment/channel/guide/{channelMethodName}")
    suspend fun channelGuide(@Path("channelMethodName") channelMethodName: String): Map<String, Any>

    @POST("payment/transaction/transfer_inq")
    suspend fun transferInquiry(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/transaction/transfer_trx")
    suspend fun transferTransaction(@Body request: Map<String, Any>): Map<String, Any>

    // --- Invoice sekolah / SPP ---
    @GET("transaction/school-invoice/unpaid")
    suspend fun unpaidSchoolInvoices(): Map<String, Any>

    @GET("transaction/school-invoice/paid")
    suspend fun paidSchoolInvoices(): Map<String, Any>

    @GET("transaction/school-invoice/process")
    suspend fun processingSchoolInvoices(): Map<String, Any>

    @POST("transaction/school-invoice/pay")
    suspend fun paySchoolInvoice(@Body request: Map<String, Any>): Map<String, Any>

    @GET("transaction/payment-service")
    suspend fun paymentServices(): Map<String, Any>

    @GET("payment/transaction/invoice")
    suspend fun paymentInvoice(): Map<String, Any>

    @POST("payment/transaction/spp_cancel_invoice")
    suspend fun cancelSppInvoice(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/transaction/transaction_spp")
    suspend fun transactionSpp(@Body request: Map<String, Any>): Map<String, Any>

    @GET("payment/channel/spp")
    suspend fun sppChannels(): Map<String, Any>

    @GET("payment/transaction/history_school")
    suspend fun schoolTransactionHistory(): Map<String, Any>

    @POST("payment/transaction/spp_trx")
    suspend fun sppTransaction(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/transaction/bill_trx")
    suspend fun billTransaction(@Body request: Map<String, Any>): Map<String, Any>

    // --- Riwayat ---
    @GET("payment/transaction/history")
    suspend fun transactionHistory(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("payment/transaction/history/id/{id}")
    suspend fun transactionHistoryDetail(@Path("id") id: String): Map<String, Any>
}
