package com.onradar.todak.core.network

import com.onradar.todak.BuildConfig
import com.onradar.todak.core.session.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    val json = Json {
        ignoreUnknownKeys = true // 백엔드가 필드를 추가해도 앱이 깨지지 않게
        explicitNulls = false     // null 필드는 요청에서 빼고 보냄 (아침엔 mealCount 없음)
        coerceInputValues = true
    }

    fun createApi(sessionStore: SessionStore): TodakApi {
        val deviceTokenInterceptor = Interceptor { chain ->
            val token = sessionStore.deviceToken
            val request = if (token != null) {
                chain.request().newBuilder().header("X-Device-Token", token).build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(deviceTokenInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                },
            )
            // Render 무료 서버가 잠에서 깨는 데 시간이 걸려 넉넉히 둡니다.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.CORE_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TodakApi::class.java)
    }
}
