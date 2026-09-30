package id.diskola.app.di.module

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.diskola.app.apiservice.AbsensiApiService
import id.diskola.app.apiservice.AsesmenApiService
import id.diskola.app.apiservice.AuthApiService
import id.diskola.app.apiservice.CommonApiService
import id.diskola.app.apiservice.KlaspayApiService
import id.diskola.app.apiservice.LogFileInterceptor
import id.diskola.app.apiservice.MateriApiService
import id.diskola.app.apiservice.RequestInterceptor
import id.diskola.app.apiservice.ResponseInterceptor
import id.diskola.app.BuildConfig
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    @JvmStatic
    fun provideRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapter.Factory
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addCallAdapterFactory(callAdapterFactory)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()

    @Provides
    @Singleton
    @JvmStatic
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    @JvmStatic
    fun provideMateriApiService(retrofit: Retrofit): MateriApiService =
        retrofit.create(MateriApiService::class.java)

    @Provides
    @Singleton
    @JvmStatic
    fun provideAbsensiApiService(retrofit: Retrofit): AbsensiApiService =
        retrofit.create(AbsensiApiService::class.java)

    @Provides
    @Singleton
    @JvmStatic
    fun provideCommonApiService(retrofit: Retrofit): CommonApiService =
        retrofit.create(CommonApiService::class.java)

    @Provides
    @Singleton
    @JvmStatic
    fun provideAsesmenApiService(retrofit: Retrofit): AsesmenApiService =
        retrofit.create(AsesmenApiService::class.java)

    @Provides
    @Singleton
    @JvmStatic
    fun provideKlaspayApiService(retrofit: Retrofit): KlaspayApiService =
        retrofit.create(KlaspayApiService::class.java)

    @Provides
    @Singleton
    @JvmStatic
    fun provideCallAdapterFactory(): CallAdapter.Factory = object : CallAdapter.Factory() {
        override fun get(
            returnType: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): CallAdapter<*, *>? {
            if (getRawType(returnType) != retrofit2.Call::class.java) {
                return null
            }

            annotations.forEach {
                if (it.annotationClass == Timeout::class) {
                    val timeout = it as Timeout
                    Timber.e("set custom timeout for api with @Timeout annotation, timeout: $timeout")
                    val delegate = retrofit.nextCallAdapter(this, returnType, annotations)
                    return object : CallAdapter<Any, retrofit2.Call<Any>> {
                        override fun responseType(): Type = delegate.responseType()

                        override fun adapt(call: retrofit2.Call<Any>): retrofit2.Call<Any> {
                            call.timeout().timeout(timeout.value.toLong(), timeout.unit)
                            return call
                        }
                    }
                }
            }

            return null
        }
    }

    @Provides
    @Singleton
    @JvmStatic
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message ->
            Timber.tag("API_REQUEST").e(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    @JvmStatic
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        requestInterceptor: RequestInterceptor,
        responseInterceptor: ResponseInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        logFileInterceptor: LogFileInterceptor,
        chuckInterceptor: ChuckerInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(chuckInterceptor) // Chucker di atas agar mencatat semua request sebelum diproses interceptor lain
        .addInterceptor(
            MockApiInterceptor(
                realBaseUrl = BuildConfig.API_URL.toHttpUrl(),
                mockBaseUrl = "https://dev.api.diskola.id/api/".toHttpUrl()
            )
        )
        .addInterceptor(requestInterceptor)
        .addInterceptor(responseInterceptor)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(logFileInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .cache(Cache(context.cacheDir, 10 * 1024 * 1024))
        .build()

    @Provides
    @Singleton
    @JvmStatic
    fun providechuckInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        val collector = ChuckerCollector(
            context = context,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )

        return ChuckerInterceptor.Builder(context)
            .collector(collector)
            .maxContentLength(250_000L)
            .alwaysReadResponseBody(true)
            .build()
    }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Timeout(val value: Int, val unit: TimeUnit)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class LogFile

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class UseMock
