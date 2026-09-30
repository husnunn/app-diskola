package id.diskola.app.apiservice

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface AkunApiService {

    @GET("mobile/app/accounts/user/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Int): Map<String, Any>

    @GET("mobile/app/accounts/get-username/{username}")
    suspend fun checkUsername(@Path("username") username: String): Map<String, Any>

    @GET("mobile/app/accounts/search-username")
    suspend fun searchUsername(@Query("q") q: String): Map<String, Any>

    @GET("mobile/app/accounts/user/{userId}/feed-post")
    suspend fun getUserFeedPosts(
        @Path("userId") userId: Int,
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/accounts/user/{userId}/feed-image")
    suspend fun getUserFeedImages(
        @Path("userId") userId: Int,
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/accounts/user/{userId}/feed-ebook")
    suspend fun getUserFeedEbooks(
        @Path("userId") userId: Int,
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @Multipart
    @POST("mobile/app/accounts/user/{userUuid}/change-avatar")
    suspend fun changeAvatar(
        @Path("userUuid") userUuid: String,
        @Part avatar: MultipartBody.Part
    ): Map<String, Any>

    @PUT("mobile/app/accounts/user/{userUuid}/change-profile")
    suspend fun updateProfile(
        @Path("userUuid") userUuid: String,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @PUT("mobile/app/accounts/user/{userUuid}/change-password")
    suspend fun changePassword(
        @Path("userUuid") userUuid: String,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @GET("mobile/app/accounts/user/shipping-address")
    suspend fun getShippingAddress(): Map<String, Any>

    @POST("mobile/app/accounts/user/shipping-address")
    suspend fun setShippingAddress(@Body request: Map<String, Any>): Map<String, Any>
}
