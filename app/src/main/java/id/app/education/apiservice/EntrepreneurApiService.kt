package id.app.education.apiservice

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet.
 * The reference API also lists a buyer transaction-accept call made against an absolute URL
 * (not the configured base URL) — omitted here since its exact path wasn't specified.
 */
interface EntrepreneurApiService {

    // --- Katalog & pencarian ---
    @GET("mobile/enterpreneur/homepage")
    suspend fun homepage(): Map<String, Any>

    @GET("mobile/enterpreneur/category")
    suspend fun categories(): Map<String, Any>

    @GET("mobile/enterpreneur/category/{categoryId}/detail")
    suspend fun categoryDetail(@Path("categoryId") categoryId: String): Map<String, Any>

    @GET("mobile/enterpreneur/goodies/categories/{categoryId}{categorySubId}")
    suspend fun goodiesSubcategories(
        @Path("categoryId") categoryId: String,
        @Path("categorySubId") categorySubId: String
    ): Map<String, Any>

    @GET("mobile/enterpreneur/card")
    suspend fun cards(): Map<String, Any>

    @GET("mobile/enterpreneur/goodies/{goodieId}/detail")
    suspend fun goodieDetail(@Path("goodieId") goodieId: String): Map<String, Any>

    @GET("mobile/enterpreneur/goodies/{goodieId}/review")
    suspend fun goodieReview(@Path("goodieId") goodieId: String): Map<String, Any>

    @GET("mobile/enterpreneur/goodies/{goodieId}/listreview")
    suspend fun goodieReviewList(@Path("goodieId") goodieId: String): Map<String, Any>

    @GET("mobile/enterpreneur/filter/count")
    suspend fun filterCount(): Map<String, Any>

    @GET("mobile/enterpreneur/filter/list")
    suspend fun filterList(): Map<String, Any>

    @GET("mobile/enterpreneur/search-merchants")
    suspend fun searchMerchants(@Query("q") q: String): Map<String, Any>

    @GET("mobile/enterpreneur/search-goodies-suggestion")
    suspend fun searchGoodiesSuggestion(@Query("q") q: String): Map<String, Any>

    @GET("mobile/enterpreneur/search")
    suspend fun search(@Query("q") q: String): Map<String, Any>

    // --- Merchant publik ---
    @GET("mobile/enterpreneur/merchants/{sellerId}")
    suspend fun merchantProfile(@Path("sellerId") sellerId: String): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/{sellerId}/goodies-all")
    suspend fun merchantAllGoodies(@Path("sellerId") sellerId: String): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/{sellerId}/goodies-best-seller")
    suspend fun merchantBestSellerGoodies(@Path("sellerId") sellerId: String): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/{sellerId}/summary")
    suspend fun merchantSummary(@Path("sellerId") sellerId: String): Map<String, Any>

    // --- Akun merchant (seller) ---
    @GET("mobile/enterpreneur/merchants/account/profile")
    suspend fun myMerchantProfile(): Map<String, Any>

    @POST("mobile/enterpreneur/merchants")
    suspend fun createMerchant(@Body request: Map<String, Any>): Map<String, Any>

    @PUT("mobile/enterpreneur/merchants/account/profiles")
    suspend fun updateMerchantProfile(@Body request: Map<String, Any>): Map<String, Any>

    @Multipart
    @POST("mobile/enterpreneur/merchants/account/profiles/image")
    suspend fun updateMerchantProfileImage(@Part image: MultipartBody.Part): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/profile/goodies-all")
    suspend fun myGoodiesAll(): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/profile/goodies-best-seller")
    suspend fun myBestSellerGoodies(): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/profile/summary")
    suspend fun myMerchantSummary(): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/profile/summary-purchase")
    suspend fun myPurchaseSummary(): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/goodies/{goodieId}")
    suspend fun viewMyGoodie(@Path("goodieId") goodieId: String): Map<String, Any>

    @Multipart
    @POST("mobile/enterpreneur/merchants/account/goodies/create")
    suspend fun createGoodie(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Map<String, Any>

    @Multipart
    @POST("mobile/enterpreneur/merchants/account/goodies/create/{goodieId}/image")
    suspend fun addGoodieImage(
        @Path("goodieId") goodieId: String,
        @Part image: MultipartBody.Part
    ): Map<String, Any>

    @Multipart
    @POST("mobile/enterpreneur/merchants/account/goodies/update/{goodieId}/image/{imageId}")
    suspend fun updateGoodieImage(
        @Path("goodieId") goodieId: String,
        @Path("imageId") imageId: String,
        @Part image: MultipartBody.Part
    ): Map<String, Any>

    @DELETE("mobile/enterpreneur/merchants/account/goodies/delete/{goodieId}/image/{imageId}")
    suspend fun deleteGoodieImage(
        @Path("goodieId") goodieId: String,
        @Path("imageId") imageId: String
    ): Map<String, Any>

    @PUT("mobile/enterpreneur/merchants/account/goodies/publish/{goodieId}")
    suspend fun publishGoodie(@Path("goodieId") goodieId: String): Map<String, Any>

    @PUT("mobile/enterpreneur/merchants/account/goodies/update/{goodieId}")
    suspend fun updateGoodie(
        @Path("goodieId") goodieId: String,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @DELETE("mobile/enterpreneur/merchants/account/goodies/delete/{goodieId}")
    suspend fun deleteGoodie(@Path("goodieId") goodieId: String): Map<String, Any>

    // --- Transaksi seller & buyer ---
    @GET("mobile/enterpreneur/merchants/account/transactions/incoming")
    suspend fun incomingTransactions(): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/transactions/processed")
    suspend fun processedTransactionsSeller(): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/transactions/completed")
    suspend fun completedTransactionsSeller(): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/transactions/{transaksiId}")
    suspend fun sellerTransactionDetail(@Path("transaksiId") transaksiId: String): Map<String, Any>

    @POST("mobile/enterpreneur/merchants/account/transactions/{TransaksiId}/accept")
    suspend fun acceptTransaction(@Path("TransaksiId") transaksiId: String): Map<String, Any>

    @POST("mobile/enterpreneur/merchants/account/transactions/{TransaksiId}/reject")
    suspend fun rejectTransaction(@Path("TransaksiId") transaksiId: String): Map<String, Any>

    @GET("mobile/enterpreneur/transactions/{TransaksiId}/cancel")
    suspend fun cancelTransactionBuyer(@Path("TransaksiId") transaksiId: String): Map<String, Any>

    @GET("mobile/enterpreneur/transactions/purchases-done")
    suspend fun purchasesDone(): Map<String, Any>

    @GET("mobile/enterpreneur/transactions/purchases-processed")
    suspend fun purchasesProcessed(): Map<String, Any>

    @GET("mobile/enterpreneur/transactions/purchases/{transaksiId}")
    suspend fun purchaseDetail(@Path("transaksiId") transaksiId: String): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/reviews")
    suspend fun sellerReviews(): Map<String, Any>

    @GET("mobile/enterpreneur/reviews")
    suspend fun buyerReviews(): Map<String, Any>

    @GET("mobile/enterpreneur/reviews/transactions/{transaksiId}")
    suspend fun buyerReviewDetail(@Path("transaksiId") transaksiId: String): Map<String, Any>

    @GET("mobile/enterpreneur/merchants/account/reviews/transactions/{transaksiId}")
    suspend fun sellerReviewDetail(@Path("transaksiId") transaksiId: String): Map<String, Any>

    @POST("mobile/enterpreneur/reviews/{goodyReviewId}")
    suspend fun postBuyerReview(
        @Path("goodyReviewId") goodyReviewId: String,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @POST("mobile/enterpreneur/merchants/account/reviews/{goodyReviewId}")
    suspend fun postSellerReview(
        @Path("goodyReviewId") goodyReviewId: String,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @GET("mobile/enterpreneur/trackings/{transaksiId}")
    suspend fun trackingInfo(@Path("transaksiId") transaksiId: String): Map<String, Any>

    @GET("mobile/enterpreneur/transactions/awb/{transaksiId}")
    suspend fun inputAwb(@Path("transaksiId") transaksiId: String): Map<String, Any>

    // --- Cart & checkout ---
    @GET("mobile/enterpreneur/carts")
    suspend fun getCart(): Map<String, Any>

    @POST("mobile/enterpreneur/carts")
    suspend fun addToCart(@Body request: Map<String, Any>): Map<String, Any>

    @PUT("mobile/enterpreneur/carts/goodies/{goods}")
    suspend fun updateCartItem(
        @Path("goods") goods: String,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @DELETE("mobile/enterpreneur/carts/goodies/{goods}")
    suspend fun removeCartItem(@Path("goods") goods: String): Map<String, Any>

    @GET("mobile/enterpreneur/checkouts/shipping-fee-list")
    suspend fun shippingFeeList(): Map<String, Any>

    @POST("mobile/enterpreneur/checkouts/transaction-create")
    suspend fun checkout(@Body request: Map<String, Any>): Map<String, Any>

    // --- Lokasi ---
    @GET("mobile/enterpreneur/location/province")
    suspend fun provinces(): Map<String, Any>

    @GET("mobile/enterpreneur/location/city")
    suspend fun cities(@Query("province_id") provinceId: String? = null): Map<String, Any>

    @GET("mobile/enterpreneur/location/district")
    suspend fun districts(@Query("city_id") cityId: String? = null): Map<String, Any>
}
