package com.zheting.mobile.core.network

import com.zheting.mobile.core.storage.PreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 会话状态持有者：API base 与会话 cookie。
 *
 * 拦截器只读内存态，写操作先更新内存再异步落盘（PreferenceStore），
 * 与网页端的 apiSession 语义一致；cookie 的提取/合并规则见 CookieProcessor。
 */
class SessionManager(
    private val prefs: PreferenceStore,
    private val scope: CoroutineScope,
) {
    @Volatile
    private var apiBase: String = DEFAULT_API_BASE

    @Volatile
    private var apiCookie: String = ""

    fun getBase(): String = apiBase

    fun setBase(base: String) {
        apiBase = base
        scope.launch { prefs.setApiBase(base) }
    }

    fun getCookieForRequest(): String = CookieProcessor.normalizeCookieForRequest(apiCookie)

    fun hasLoginCookie(): Boolean = CookieProcessor.hasLoginCookie(apiCookie)

    fun setCookie(cookie: String) {
        if (cookie == apiCookie) return
        apiCookie = cookie
        persistCookie(cookie)
    }

    fun clearCookie() {
        apiCookie = ""
        persistCookie("")
    }

    /**
     * 从响应更新会话（契约 §1.4，两条来源都要）：
     * 1. 响应头 Set-Cookie（[setCookieHeader] 已由拦截器截 `;` 前部分）；
     * 2. body 顶层 cookie / data.cookie。
     * 头优先于 body（对齐 api.rs collect_set_cookie）；合并后不含 MUSIC_U 不写入。
     */
    fun saveCookieFromResponse(data: JsonElement, setCookieHeader: String = "") {
        val raw = if (setCookieHeader.isNotBlank()) {
            setCookieHeader
        } else {
            runCatching { data.cookieCandidates() }.getOrDefault("")
        }
        val extracted = CookieProcessor.extractCookie(raw)
        if (extracted.isEmpty() || extracted == apiCookie) return
        val merged = CookieProcessor.mergeCookies(apiCookie, extracted)
        if (!CookieProcessor.hasLoginCookie(merged)) return
        setCookie(merged)
    }

    /** 启动时从磁盘恢复会话。应在进程启动早期完成。 */
    suspend fun init() {
        apiBase = prefs.apiBase().firstOrNull().takeUnless { it.isNullOrEmpty() } ?: DEFAULT_API_BASE
        apiCookie = prefs.apiCookie().firstOrNull().orEmpty()
    }

    private fun persistCookie(cookie: String) {
        scope.launch { prefs.setApiCookie(cookie) }
    }

    companion object {
        /** 与原项目 DEFAULT_API_BASE 保持一致。 */
        const val DEFAULT_API_BASE = "https://music.xubuyuan.top"
    }
}

private fun JsonElement.cookieCandidates(): String {
    val obj = this as? JsonObject ?: return ""
    val top = obj["cookie"]?.jsonPrimitive?.contentOrNull ?: ""
    if (top.isNotEmpty()) return top
    return (obj["data"] as? JsonObject)?.get("cookie")?.jsonPrimitive?.contentOrNull ?: ""
}