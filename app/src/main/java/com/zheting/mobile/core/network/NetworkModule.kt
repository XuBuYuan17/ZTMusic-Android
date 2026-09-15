package com.zheting.mobile.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络组件装配。Json 与客户端实例只构造一次，通过 AppContainer 注入。
 *
 * baseUrl 为占位地址（SessionInterceptor 会按 SessionManager 动态替换），
 * 但 Retrofit 要求 http(s) scheme 且以 / 结尾。
 *
 * 安全约束（契约 §1.6）：请求/响应一律不加日志拦截器——cookie、密码与
 * 敏感参数不得落日志。如需抓包请在系统层用代理工具。
 */
object NetworkModule {

    const val RETROFIT_PLACEHOLDER_BASE = "https://music.xubuyuan.top/"

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun okHttpClient(
        session: SessionManager,
        json: Json = this.json,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(SessionInterceptor(session, json))
            .addInterceptor(SafeReadRetryInterceptor())
        return builder.build()
    }

    fun retrofit(okHttpClient: OkHttpClient, json: Json = this.json): Retrofit =
        Retrofit.Builder()
            .baseUrl(RETROFIT_PLACEHOLDER_BASE)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}