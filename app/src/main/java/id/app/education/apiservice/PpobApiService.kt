package id.app.education.apiservice

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface PpobApiService {

    // --- Katalog produk ---
    @GET("payment/product/list/pulsa/{phone}")
    suspend fun pulsaProducts(@Path("phone") phone: String): Map<String, Any>

    @GET("payment/product/list/pulsa_pasca/{phone}")
    suspend fun pulsaPascaProducts(@Path("phone") phone: String): Map<String, Any>

    @GET("payment/product/list/pln_prabayar")
    suspend fun plnPrepaidProducts(): Map<String, Any>

    @GET("payment/product/list/pln_pascabayar")
    suspend fun plnPostpaidProducts(): Map<String, Any>

    @GET("payment/product/list/pdam")
    suspend fun pdamProducts(): Map<String, Any>

    @GET("payment/product/list/internet")
    suspend fun internetProducts(): Map<String, Any>

    @GET("payment/product/list/bpjs")
    suspend fun bpjsProducts(): Map<String, Any>

    @GET("payment/product/list/game")
    suspend fun gameProducts(): Map<String, Any>

    @GET("payment/product/list/game/voucher")
    suspend fun gameVoucherProducts(): Map<String, Any>

    @GET("payment/product/list/streaming")
    suspend fun streamingProducts(): Map<String, Any>

    // --- Transaksi PPOB ---
    @POST("payment/transaction/ppob/inq")
    suspend fun ppobInquiry(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/transaction/ppob/inq_check")
    suspend fun ppobInquiryCheck(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/transaction/ppob/inq_pay")
    suspend fun ppobInquiryPay(@Body request: Map<String, Any>): Map<String, Any>

    @POST("payment/transaction/ppob/trx")
    suspend fun ppobTransaction(@Body request: Map<String, Any>): Map<String, Any>
}
