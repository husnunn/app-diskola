package id.diskola.app.apiservice

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface SosmedApiService {

    @Multipart
    @POST("mobile/app/schools/feed-posts")
    suspend fun createFeedPost(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part images: List<MultipartBody.Part>?
    ): Map<String, Any>

    @Multipart
    @POST("mobile/app/schools/feed-ebooks")
    suspend fun createFeedEbook(
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?
    ): Map<String, Any>

    @GET("mobile/app/schools/feed-posts")
    suspend fun getFeedPosts(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/schools/feed-ebooks")
    suspend fun getFeedEbooks(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/schools/feeds/{feedId}")
    suspend fun getFeedDetail(@Path("feedId") feedId: String): Map<String, Any>

    @GET("mobile/app/schools/feeds/{feedId}/comment")
    suspend fun getFeedComments(@Path("feedId") feedId: String): Map<String, Any>

    @GET("mobile/app/schools/feeds/{feedId}/like")
    suspend fun getFeedLikes(@Path("feedId") feedId: String): Map<String, Any>

    @POST("mobile/app/schools/feed-posts/send-like")
    suspend fun likeFeedPost(@Body request: Map<String, Any>): Map<String, Any>

    @DELETE("mobile/app/schools/feed-posts/{feedId}/unlike")
    suspend fun unlikeFeedPost(@Path("feedId") feedId: String): Map<String, Any>

    @POST("mobile/app/schools/feed-posts/{feedId}/comment")
    suspend fun commentOnFeedPost(
        @Path("feedId") feedId: String,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @DELETE("mobile/app/schools/feeds/{feedId}")
    suspend fun deleteFeed(@Path("feedId") feedId: String): Map<String, Any>

    @GET("mobile/app/schools/explore/feed")
    suspend fun exploreFeed(
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): Map<String, Any>

    @GET("mobile/app/schools/explore/user")
    suspend fun exploreUser(@Query("q") q: String? = null): Map<String, Any>

    @GET("mobile/app/schools/explore/hastag")
    suspend fun exploreHashtag(@Query("q") q: String? = null): Map<String, Any>
}
