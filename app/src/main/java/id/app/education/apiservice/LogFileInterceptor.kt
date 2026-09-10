package id.app.education.apiservice

import id.app.education.utils.Utils
import id.app.education.di.module.LogFile
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import retrofit2.Invocation
import timber.log.Timber
import java.io.EOFException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class LogFileInterceptor @Inject constructor(private val utils: Utils) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val responseCode = response.code
        val responseBody = response.body

        val tag = request.tag(Invocation::class.java)
//        Timber.e("url: ${request.url}\nannotation: ${tag?.method()?.annotations?.map { it.annotationClass.simpleName }}")

        tag?.method()?.getAnnotation(LogFile::class.java)?.let {
            Timber.e("Log api with @LogFile annotation")
            utils.logFile(buildString {
                append("requesting: [${request.method}] ${request.url}\n")
                request.headers.forEach {
                    append("${it.first}: ${it.second}\n")
                }

                val requestBody = request.body
                if (requestBody != null) {
                    val buffer = Buffer()
                    requestBody.writeTo(buffer)

                    val contentType = requestBody.contentType()
                    val charset: Charset =
                        contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

                    append("")
                    if (buffer.isProbablyUtf8()) {
                        append(buffer.readString(charset))
                        append("--> END ${request.method} (${requestBody.contentLength()}-byte body)")
                    }
                }
            }.also { Timber.e("request data: $it") })

            utils.logFile(buildString {
                append("response: [$responseCode - ${response.request.method}] ${response.request.url}\n")
                append(
                    responseBody?.source()?.also { it.request(Long.MAX_VALUE) }?.buffer?.clone()
                        ?.readString(Charset.forName("UTF-8"))
                )
            })
        }

        return response
    }
}

fun Buffer.isProbablyUtf8(): Boolean {
    try {
        val prefix = Buffer()
        val byteCount = size.coerceAtMost(64)
        copyTo(prefix, 0, byteCount)
        for (i in 0 until 16) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    } catch (_: EOFException) {
        return false // Truncated UTF-8 sequence.
    }
}
