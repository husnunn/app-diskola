package id.app.education.di.module

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class MockApiInterceptor(
    private val realBaseUrl: HttpUrl,
    private val mockBaseUrl: HttpUrl
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val useMock = request.header("Use-Mock") == "true"

        val newUrl = if (useMock) {
            Timber.d("Using MOCK API for ${request.url}")
            mockBaseUrl.newBuilder()
                .encodedPath(request.url.encodedPath)
                .query(request.url.query)
                .build()
        } else {
            request.url
        }

        val newRequest = request.newBuilder()
            .url(newUrl)
            .removeHeader("Use-Mock") // agar tidak dikirim ke server
            .build()

        return chain.proceed(newRequest)
    }
}
