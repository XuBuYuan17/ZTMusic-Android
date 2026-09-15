package com.zheting.mobile.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.parseToJsonElement
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * 请求/响应统一处理，规则逐条对齐原项目 src/lib/api/client.ts 的 request()：
 * 1. 用 SessionManager 的 base 替换请求域名（base 可配置）；
 * 2. randomCNIP=true：GET 注入 query，有 body 的 POST 追加进 form；登录/登出接口显式关闭；
 * 3. 会话 cookie：GET 注入 query、有 body 的 POST 追加进 form；QR 辅助接口 noCookie 不下发；
 *    POST 无 body（logout 等）不注入任何参数，与原项目一致；
 * 4. 响应整体读取后回调 SessionManager 提取并合并 cookie（body + 响应头 Set-Cookie）。
 */
class SessionInterceptor(
    private val session: SessionManager,
    private val json: Json,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val isPost = original.method == "POST"

        // 1) 动态 base
        val baseText = session.getBase().takeIf { it.isNotEmpty() } ?: SessionManager.DEFAULT_API_BASE
        val target = runCatching { baseText.toHttpUrl() }.getOrNull() ?: original.url
        val sb = original.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)

        val skipRandomCNIP = path.startsWith("/login") || path == "/logout"
        val cookie = if (path.startsWith("/login/qr/")) "" else session.getCookieForRequest()

        // 2) + 3)
        val builder: Request.Builder = when {
            isPost && original.body != null -> {
                val originalBody = original.body as FormBody
                val form = FormBody.Builder().apply {
                    for (i in 0 until originalBody.size) {
                        addEncoded(originalBody.encodedName(i), originalBody.encodedValue(i))
                    }
                    if (!skipRandomCNIP) add("randomCNIP", "true")
                    if (cookie.isNotEmpty()) add("cookie", cookie)
                }.build()
                original.newBuilder().url(sb.build()).method("POST", form)
            }
            isPost -> {
                // 无 body 的 POST：不注入任何参数
                original.newBuilder().url(sb.build())
            }
            else -> {
                if (!skipRandomCNIP) sb.addQueryParameter("randomCNIP", "true")
                if (cookie.isNotEmpty()) sb.addQueryParameter("cookie", cookie)
                original.newBuilder().url(sb.build())
            }
        }

        val response = chain.proceed(builder.build())

        // 4) 收集响应头 Set-Cookie（截 `;` 前部分、剔除空项，对齐 api.rs collect_set_cookie），
        //    与 body cookie 一并交给 SessionManager 合并（头优先）。
        val setCookieHeader = response.headers("Set-Cookie")
            .mapNotNull { it.substringBefore(';').trim().takeIf(String::isNotEmpty) }
            .joinToString("; ")

        // 5) 提取并合并响应 cookie（body 需整体读取才能拿到，原网页端 fetch 亦整体解析）
        return runCatching {
            val bodyString = response.body?.string() ?: return@runCatching response
            json.parseToJsonElement(bodyString).let { session.saveCookieFromResponse(it, setCookieHeader) }
            response.newBuilder()
                .body(bodyString.toResponseBody(response.body?.contentType()))
                .build()
        }.getOrElse { response }
    }
}