package com.luno.core.network.di

import com.luno.core.network.api.LunoApiService
import com.luno.core.network.interceptor.AuthInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Note: This is a placeholder for DI (like Hilt/Koin).
// You can use this as a manual dependency provider or convert to Hilt/Koin module later.
@Suppress("unused")
object NetworkModule {
    private const val BASE_URL = "https://api.luno.example.com/" // Replace with actual backend URL
    private const val TIMEOUT = 30L

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Use NONE in production
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun provideLunoApiService(retrofit: Retrofit): LunoApiService {
        return retrofit.create(LunoApiService::class.java)
    }
}