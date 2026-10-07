package id.diskola.app.apiservice

import android.annotation.SuppressLint
import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import id.diskola.app.utils.PreferenceClass
import id.diskola.app.utils.session.SessionEvent
import id.diskola.app.utils.session.SessionEvents
import id.diskola.app.utils.session.SessionKeys
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject

@Suppress("UNCHECKED_CAST")
class ResponseInterceptor @Inject constructor(
    @ApplicationContext val context: Context,
    moshi: Moshi,
    private val preference: PreferenceClass,
    private val sessionEvents: SessionEvents,
) : Interceptor {

    private val errorAdapter by lazy {
        moshi.adapter<Map<String, Any>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
    }

    private val defaultErrorMsg by lazy {
        "Terjadi gangguan pada koneksi internet Anda, silahkan ulangi beberapa saat lagi"
    }

    @SuppressLint("SetTextI18n")
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val responseCode = response.code
        val responseBody = response.body

        // 401 handling is centralized (doc `02-auth-login-sesi.md` §9, decision Q6): this
        // interceptor only reports the event — it has no Activity/Composable of its own to show a
        // dialog from, and logging out here directly (as the legacy per-screen handling and the
        // previous version of this class both did) would skip the pending-AKM-exam check that
        // `SessionManager.logout` must run first (`docs/FLOW_QUESTIONS.md` bagian E#2). A collector
        // in `AppNavHost` shows "Sesi Berakhir" and calls `SessionManager.logout()` itself.
        if (responseCode == 401) {
            responseBody?.close()
            if (preference.getBoolean(SessionKeys.LOGGED_IN)) {
                sessionEvents.emit(SessionEvent.Unauthorized)
            }
            throw ApiException("Unauthorized", 401)
        }

        // Binary responses (images/media/files via CommonApiService.download) must never be
        // buffered as UTF-8 text: readString() replaces every byte sequence that isn't valid UTF-8
        // with U+FFFD, and reconstructing the body from that string afterwards re-encodes those
        // replacement characters — irreversibly corrupting the payload (confirmed live: a
        // downloaded question image's JPEG header `FF D8 FF E0` came back as repeated `EF BF BD`,
        // U+FFFD's UTF-8 encoding). Detect this by content-type's top-level type rather than
        // assuming everything is JSON; unknown/missing content-type falls through to the existing
        // JSON-parsing path unchanged, since every real API endpoint here does set one.
        val topLevelType = responseBody?.contentType()?.type
        val isBinary = topLevelType == "image" || topLevelType == "video" || topLevelType == "audio" ||
            responseBody?.contentType()?.subtype?.contains("octet-stream", ignoreCase = true) == true
        if (isBinary) {
            if ((responseCode / 100) == 2) return response
            responseBody?.close()
            throw ApiException(defaultErrorMsg, responseCode)
        }

        return try {
            val content = responseBody?.source()?.apply { request(Long.MAX_VALUE) }?.buffer?.clone()
                ?.readString(Charset.forName("UTF-8")) ?: defaultErrorMsg

            val contentMap: Map<String, Any> = try {
                errorAdapter.fromJson(content) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            val errorTypes: Array<String> = try {
                (contentMap["errors"] as? Map<String, *>)?.keys?.toTypedArray() ?: emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }

            // First message per field, de-duplicated — what the legacy `userFacingHttpError` showed
            // in preference to `message` (used by Agenda Mingguan's check-in/out errors).
            val validationMessages: List<String> = try {
                (contentMap["errors"] as? Map<String, *>)?.values
                    ?.mapNotNull { v ->
                        when (v) {
                            is List<*> -> v.firstOrNull() as? String
                            is String -> v
                            else -> null
                        }
                    }
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            if ((responseCode / 100) == 2) {

                val mediaType = responseBody?.contentType()
                responseBody?.close()
                val newBody = content.toResponseBody(mediaType)
                return response.newBuilder().body(newBody).build()
            } else {
                // Backend uses "message" on most endpoints but "error" on some (e.g.
                // check-account) — without this fallback the literal string "null" was thrown
                // as the message, permanently hiding the real reason from AppErrorHandler.
                val errorBody = (contentMap["message"] as? String)
                    ?: (contentMap["error"] as? String)
                    ?: defaultErrorMsg
                val errorData: Map<String, Any> = (contentMap["data"] as? Map<String, Any>) ?: emptyMap()
                val errorCode = contentMap["error_code"] as? String
                val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()

                responseBody?.close()
                throw ApiException(errorBody, responseCode, errorTypes, errorData, errorCode, retryAfterSeconds, validationMessages)
            }
        } catch (e: Exception) {
            responseBody?.close()
            Timber.e(e)
            throw e
        }
    }
}


class ApiException(
    override val message: String?,
    val responseCode: Int?,
    val errorTypes: Array<String> = emptyArray(),
    val data: Map<String, Any> = emptyMap(),
    /** Top-level `error_code` from the response body — e.g. `"DEVICE_CONFLICT"` (doc
     * `02-auth-login-sesi.md` §11), read by [id.diskola.app.utils.session.AuthErrorMapper]. */
    val errorCode: String? = null,
    val retryAfterSeconds: Long? = null,
    /** Per-field validation texts from the body's `errors` map (distinct, first per field). */
    val validationMessages: List<String> = emptyList(),
) : IOException(message)

//class ResponseInterceptor @Inject constructor(val context: Context, moshi: Moshi) : Interceptor {
//
//    private val errorAdapter by lazy {
//        moshi.adapter<Map<String, Any>>(
//            Types.newParameterizedType(
//                Map::class.java,
//                String::class.java,
//                Any::class.java
//            )
//        )
//    }
//
//    private val defaultErrorMsg by lazy { "Terjadi gangguan pada koneksi internet Anda, silahkan ulangi beberapa saat lagi" }
//
//    @SuppressLint("SetTextI18n")
//    override fun intercept(chain: Interceptor.Chain): Response {
//        val request = chain.request()
//        val response = chain.proceed(request)
//        val responseCode = response.code
//        val responseBody = response.body
//
//        return try {
//            val content =
//                responseBody?.source()?.also { it.request(Long.MAX_VALUE) }?.buffer?.clone()
//                    ?.readString(Charset.forName("UTF-8"))
//                    ?: defaultErrorMsg
//
//            if ((responseCode / 100) == 2) {
//                val contentMap: Map<String, Any> = try {
//                    errorAdapter.fromJson(content) ?: emptyMap()
//                } catch (e: Exception) {
//                    emptyMap()
//                }
//                val errorTypes: Array<String> = try {
//                    (contentMap["errors"] as? Map<String, *>)?.keys?.toTypedArray()
//                        ?: emptyArray()
//                } catch (e: Exception) {
//                    emptyArray()
//                }
//                if (contentMap.containsKey("status") && contentMap["status"] as? String != "success")
//                    throw ApiException(
//                        contentMap["message"] as? String ?: defaultErrorMsg,
//                        responseCode,
//                        errorTypes
//                    )
//                else
//                    response
//            } else {
//                if (responseCode == 503 || responseCode == 504) {
//                    try {
//
//                    } catch (e: Exception) {
//                        Timber.e("failed to open maintenance dialog, ${e.message}")
//                    }
//                }
//
//                val contentMap: Map<String, Any> = try {
//                    errorAdapter.fromJson(content) ?: emptyMap()
//                } catch (e: Exception) {
//                    emptyMap()
//                }
//
//                val errorTypes: Array<String> = try {
//                    (contentMap["errors"] as? Map<String, *>)?.keys?.toTypedArray()
//                        ?: emptyArray()
//                } catch (e: Exception) {
//                    emptyArray()
//                }
//
//                val errorData: Map<String, Any> = try {
//                    (contentMap["data"] as? Map<String, Any>) ?: emptyMap()
//                } catch (e: Exception) {
//                    emptyMap()
//                }
//
//                val errorBody = try {
//                    "${(contentMap["message"] as? String) ?: "Mohon maaf, terjadi kesalahan"}. ${
//                        ((contentMap["error"] as? String)?.plus("")) ?: ""
//                    }"
//                } catch (e: Exception) {
//                    defaultErrorMsg
//                }
//
//                responseBody?.close()
//                throw ApiException(errorBody, responseCode, errorTypes, errorData)
//            }
//        } catch (e: Exception) {
//            responseBody?.close()
//            Timber.e(e)
//            throw e
//        }
//    }
//}
