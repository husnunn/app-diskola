package id.diskola.app.apiservice

import id.diskola.app.dataclass.ResponData.CheckAccountApiResponse
import id.diskola.app.dataclass.ResponData.CheckAccountRequest
import id.diskola.app.dataclass.ResponData.LoginAccountApiResponse
import id.diskola.app.dataclass.ResponData.LoginAccountRequest
import id.diskola.app.dataclass.ResponData.LoginSsoApiResponse
import id.diskola.app.dataclass.ResponData.LoginSsoRequest
import id.diskola.app.dataclass.ResponData.LoginSsoSchoolApiResponse
import id.diskola.app.dataclass.ResponData.LoginSsoSchoolRequest
import id.diskola.app.dataclass.ResponData.ResetPasswordRequest
import id.diskola.app.dataclass.ResponData.SchoolListResponse
import id.diskola.app.dataclass.ResponData.SessionResponse
import id.diskola.app.dataclass.ResponData.SetupUserFcmRequest
import id.diskola.app.dataclass.ResponData.UserResponseItem
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

    /**
     * `name` is always sent blank on purpose (doc `02-auth-login-sesi.md` §4.2, decision Q3): the
     * legacy backend's own `name` filter is never used by the app — the school picker only ever
     * filters the locally cached pages (see `SchoolRepository`).
     */
    @GET("/api/mobile/app/authentication/schools")
    suspend fun getSchools(
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 20,
        @Query("name") name: String = ""
    ): SchoolListResponse

    @POST("/api/mobile/app/authentication/check-account")
    suspend fun checkAccount(
        @Body request: CheckAccountRequest
    ): CheckAccountApiResponse

    @POST("/api/mobile/app/authentication/login-account")
    suspend fun loginAccount(
        @Body request: LoginAccountRequest
    ): LoginAccountApiResponse

    @POST("/api/mobile/app/authentication/login-sso")
    suspend fun loginSso(@Body request: LoginSsoRequest): LoginSsoApiResponse

    @POST("/api/mobile/app/authentication/login-sso/school")
    suspend fun loginSsoSchool(@Body request: LoginSsoSchoolRequest): LoginSsoSchoolApiResponse

    @POST("/api/mobile/app/authentication/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Map<String, Any>

    @DELETE("logout")
    suspend fun logout(): Map<String, Any>

    @POST("sosmed/setting/setup-user-fcm")
    suspend fun updateFcmToken(@Body request: SetupUserFcmRequest): Map<String, Any>

    @GET("/api/mobile/app/authentication/current-user")
    suspend fun currentUser(): Map<String, Any>

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

    // --- Placeholder scaffolding below: request/response shapes not confirmed yet, no UI consumer. ---

    @GET("mobile/app/authentication/login-sso/classes")
    suspend fun ssoClasses(): Map<String, Any>

    @POST("mobile/app/authentication/login-sso/check-nisn")
    suspend fun checkNisn(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/login-sso/verification")
    suspend fun verifyNisn(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/requesting-student")
    suspend fun requestingStudent(@Body request: Map<String, Any>): Map<String, Any>

    @POST("mobile/app/authentication/requesting-teacher")
    suspend fun requestingTeacher(@Body request: Map<String, Any>): Map<String, Any>

    @GET("mobile/app/roles")
    suspend fun getRoles(): Map<String, Any>

    @GET("mobile/gmail/verify/{google_token}")
    suspend fun verifyGoogle(@Path("google_token") googleToken: String): Map<String, Any>

    @GET("mobile/email/verify")
    suspend fun verifyEmail(): Map<String, Any>
}
