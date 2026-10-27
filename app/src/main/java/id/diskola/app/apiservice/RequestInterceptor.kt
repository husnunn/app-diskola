package id.diskola.app.apiservice


import id.diskola.app.BuildConfig
import id.diskola.app.utils.PreferenceClass
import id.diskola.app.utils.Utils
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

/** What the app tells the backend about itself on every request (doc 06 §16, android-portal `e0a12b62`). */
internal const val HEADER_APP_PLATFORM = "X-App-Platform"
internal const val HEADER_APP_VERSION = "X-App-Version"
internal const val APP_PLATFORM = "android"

/**
 * `X-App-Platform: android` and `X-App-Version: <versionName>` (never `versionCode`) so the backend can tell platform and version of
 * the caller apart, e.g. to roll out a new content format such as LaTeX in answer options. Set semantics (`header`), so a header can never
 * be doubled. Kept apart from the interceptor so it is tested on the plain JVM with a real [Request].
 */
internal fun Request.Builder.withAppHeaders(versionName: String = BuildConfig.VERSION_NAME): Request.Builder =
    header(HEADER_APP_PLATFORM, APP_PLATFORM).header(HEADER_APP_VERSION, versionName)

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
            .withAppHeaders()

        if (!token.isNullOrBlank()) {
            reqBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(reqBuilder.build())
    }
}