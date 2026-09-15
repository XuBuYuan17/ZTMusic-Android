package com.zheting.mobile.core.model

/**
 * 当前登录账号在应用内的呈现。对应原项目 stores/auth.svelte.ts 的 AuthUser。
 * userId 与其他 id 字段一致使用字符串（原项目 SongId 类型收敛）。
 */
data class AuthUser(
    val userId: String,
    val nickname: String,
    val avatarUrl: String = "",
)

/**
 * 会话自检的完整结果：区分「未登录」「已登录」「请求/解析失败」。
 */
sealed interface SessionStatus {
    data object NotLoggedIn : SessionStatus
    data class LoggedIn(val user: AuthUser) : SessionStatus
    data class Error(val throwable: Throwable) : SessionStatus
}