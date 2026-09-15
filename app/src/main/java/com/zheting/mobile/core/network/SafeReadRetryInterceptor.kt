package com.zheting.mobile.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 安全读请求重试（契约 §1.5）：只对 GET、且仅连接失败 / 5xx（网关类）时
 * 有限重试（≤2 次，200ms / 400ms 退避）。业务 code 失败在响应层之后判断，不在此重试。
 */
class SafeReadRetryInterceptor(
    private val maxAttempts: Int = 2,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // 仅 GET 走重试；POST 等有副作用的请求直接透传
        if (request.method != "GET") return chain.proceed(request)

        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(request)
                val retryable = response.code in RETRYABLE_CODES
                if (!retryable || attempt >= maxAttempts) return response
                response.close()
            } catch (e: IOException) {
                if (attempt >= maxAttempts) throw e
            }
            attempt++
            Thread.sleep(BASE_DELAY_MS * attempt)
        }
    }

    companion object {
        /** 网关/服务端瞬时错误（bad gateway 等，对齐原项目重试条件）。 */
        private val RETRYABLE_CODES = setOf(502, 503, 504)
        private const val BASE_DELAY_MS = 200L
    }
}