package id.app.education.apiservice

import android.annotation.SuppressLint
import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import id.app.education.ui.MainActivity
import id.app.education.utils.IntentUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class ResponseInterceptor @Inject constructor(
    @ApplicationContext val context: Context,
    moshi: Moshi,
    private val intentUtil: Provider<IntentUtil>
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

        // Automatic logout on 401 Unauthorized
        if (responseCode == 401) {
            try {
                CoroutineScope(Dispatchers.Main).launch {
                    intentUtil.get().logOut()

                    try {
                        val it = android.content.Intent(context, MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("logout_message", "Sesi Anda berakhir. Silakan login kembali.")
                        }
                        context.startActivity(it)
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                responseBody?.close()
                throw ApiException("Unauthorized", 401)
            }
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

            if ((responseCode / 100) == 2) {

                val mediaType = responseBody?.contentType()
                responseBody?.close()
                val newBody = content.toResponseBody(mediaType)
                return response.newBuilder().body(newBody).build()
            } else {
                val errorBody = contentMap["message"] as? String ?: contentMap["message"].toString()
                val errorData: Map<String, Any> = (contentMap["data"] as? Map<String, Any>) ?: emptyMap()

                responseBody?.close()
                throw ApiException(errorBody, responseCode, errorTypes, errorData)
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
    val data: Map<String, Any> = emptyMap()
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
