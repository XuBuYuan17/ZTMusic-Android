package com.zheting.mobile.core.network

/**
 * cookie 会话的纯逻辑，逐行对齐原项目 src/lib/api/session.ts：
 * extractCookie / mergeCookies / normalizeCookieForRequest。
 *
 * 规则（保持与网页端一致）：
 * - 会话 cookie 从响应 body 的 cookie 字段提取，剔除 Path/Domain/Expires 等控制项；
 * - 合并式更新：响应 cookie 覆盖旧值同名键，保留旧值未被提及的键；
 * - 请求时只下发含 MUSIC_U 的 cookie，且保证 os=pc 存在，否则视为无有效登录态。
 */
object CookieProcessor {

    private val DROP_REGEX = Regex(
        "^(Path|Domain|Expires|Max-Age|HttpOnly|Secure|SameSite)",
        RegexOption.IGNORE_CASE,
    )

    /** 提取会话 cookie：剔除控制项。空输入返回空串。 */
    fun extractCookie(raw: String): String {
        if (raw.isEmpty()) return ""
        return raw.split(';')
            .map(String::trim)
            .filter { it.contains('=') && !DROP_REGEX.containsMatchIn(it) }
            .joinToString("; ")
    }

    /** newCookie 覆盖 oldCookie 同名键，保留 oldCookie 未被提及的键（保持插入序）。 */
    fun mergeCookies(oldCookie: String, newCookie: String): String {
        val map = LinkedHashMap<String, String>()
        fun ingest(part: String) {
            val kv = part.trim()
            val eq = kv.indexOf('=')
            if (eq > 0) map[kv.substring(0, eq)] = kv.substring(eq + 1)
        }
        oldCookie.split(';').forEach(::ingest)
        newCookie.split(';').forEach(::ingest)
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    /** 请求用规范化：无 MUSIC_U 返回空串；无 os= 时补 os=pc，与网页端保持一致。 */
    fun normalizeCookieForRequest(cookieString: String): String {
        if (cookieString.isEmpty()) return ""
        val parts = cookieString.split(';').map(String::trim).filter { it.contains('=') }
        if (!parts.any { it.startsWith("MUSIC_U=") }) return ""
        val result = parts.toMutableList()
        if (!result.any { it.startsWith("os=") }) result.add("os=pc")
        return result.joinToString("; ")
    }

    /** 是否存在视为登录的会话（请求规范化后非空）。 */
    fun hasLoginCookie(cookieString: String): Boolean =
        normalizeCookieForRequest(cookieString).isNotEmpty()
}