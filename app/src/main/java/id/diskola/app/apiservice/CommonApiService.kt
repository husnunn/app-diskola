package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.CheckVersionResponse
import id.diskola.app.dataclass.ResponData.PolicyResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url

interface CommonApiService {

    @GET("mobile/app/config/check-android-version")
    suspend fun checkVersion(): CheckVersionResponse

    @GET("mobile/app/policy")
    suspend fun policy(): PolicyResponse

    @GET("mobile/app/accounts/user/{userId}/get-layout-about")
    suspend fun about(@Path("userId") userId: Int): PolicyResponse

    @Headers("Accept: */*")
    @GET
    suspend fun download(@Url url: String): ResponseBody

    @Headers("Accept: */*")
    @GET
    suspend fun downloadAsString(@Url url: String): ResponseBody
}
