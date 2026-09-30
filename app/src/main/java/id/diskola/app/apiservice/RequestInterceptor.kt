package id.diskola.app.apiservice


import id.diskola.app.utils.PreferenceClass
import id.diskola.app.utils.Utils
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RequestInterceptor @Inject constructor(val utils: Utils, val pref: PreferenceClass) :
    Interceptor {

    private val defaultErrorMsg by lazy { "Terjadi gangguan pada koneksi internet Anda, silahkan ulangi beberapa saat lagi" }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!utils.isInternetAvailable()) {
            throw ApiException(defaultErrorMsg, 0)
        }

        val token = try { pref.getString("user_token") } catch (e: Exception) { "" }
        val reqBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            reqBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(reqBuilder.build())
    }
}