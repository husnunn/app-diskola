package id.app.education.apiservice

import id.app.education.dataclass.ResponData.CheckAccountApiResponse
import id.app.education.dataclass.ResponData.CheckAccountRequest
import id.app.education.dataclass.ResponData.LoginAccountApiResponse
import id.app.education.dataclass.ResponData.LoginAccountRequest
import id.app.education.dataclass.ResponData.SchoolListResponse
import id.app.education.dataclass.ResponData.SessionResponse
import id.app.education.dataclass.ResponData.UserResponseItem
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApiService {

    @GET("mock/mobile/profiles")
    suspend fun getProfile(): UserResponseItem

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body body: Map<String, Any>
    ): Map<String, String>

    @GET("/api/mobile/app/authentication/schools")
    suspend fun getSchools(
        @Query("q") q: String,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 10,
        @Query("name") name: String? = null
    ): SchoolListResponse

    @POST("/api/mobile/app/authentication/check-account")
    suspend fun checkAccount(
        @Body request: CheckAccountRequest
    ): CheckAccountApiResponse

    @POST("/api/mobile/app/authentication/login-account")
    suspend fun loginAccount(
        @Body request: LoginAccountRequest
    ): LoginAccountApiResponse

    @DELETE("sessions/others")
    suspend fun logoutOthers()

    @DELETE("sessions/{id}")
    suspend fun logoutDevice(@Path("id") id: Int)

    @GET("sessions")
    suspend fun listSessions(
        @Query("limit") limit: Int = 10,
        @Query("take") take: Int = 10,
        @Query("skip") skip: Int = 0
    ): SessionResponse

    // --- Placeholder scaffolding below: request/response shapes not yet defined, no UI consumer yet. ---

    @POST("mobile/app/authentication/login-sso")
    suspend fun loginSso(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/app/authentication/login-sso/classes")
    suspend fun ssoClasses(): Map<String, Any>

    @POST("mobile/app/authentication/login-sso/school")
    suspend fun loginSsoSchool(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/login-sso/check-nisn")
    suspend fun checkNisn(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/login-sso/verification")
    suspend fun verifyNisn(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/requesting-student")
    suspend fun requestingStudent(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/requesting-teacher")
    suspend fun requestingTeacher(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/reset-password")
    suspend fun resetPassword(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/app/roles")
    suspend fun getRoles(): Map<String, Any>

    @GET("mobile/gmail/verify/{google_token}")
    suspend fun verifyGoogle(@Path("google_token") googleToken: String): Map<String, Any>

    @GET("mobile/email/verify")
    suspend fun verifyEmail(): Map<String, Any>

    @DELETE("logout")
    suspend fun logout(): Map<String, Any>

    @POST("sosmed/setting/setup-user-fcm")
    suspend fun updateFcmToken(@Body request: Map<String, Any>): Map<String, Any>
}
