package com.company.salesapp.network

import android.content.Context
import com.company.salesapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        return getRetrofit(context).create(ApiService::class.java)
    }

    private fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val tokenStore = TokenStore.getInstance(context)

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = tokenStore.getToken()
            val request = if (!token.isNullOrBlank()) {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .build()
            } else {
                original.newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        // BuildConfig.BASE_URL diset di app/build.gradle.
        // DEV: http://10.0.2.2:8000/  (Laragon lokal via emulator)
        // PROD: nanti diganti https://domain-perusahaan.com/
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Dipanggil bila BASE_URL perlu di-override runtime (mis. multi-environment switch di app). */
    fun resetForNewBaseUrl() {
        retrofit = null
    }
}
