package com.zheting.mobile.core.network

import kotlinx.serialization.Serializable

/**
 * 登录态相关 DTO。网易云响应 HTTP 恒 200，成败看 code（可能在顶层也可能在 data 内）。
 * 字段只声明需要的，其余忽略（Json 配置 ignoreUnknownKeys）。
 *
 * 结构对齐原项目 src/lib/stores/auth.svelte.ts 消费的 /login/status 与 /user/account 响应。
 */
@Serializable
data class AccountResultDto(
    val code: Int? = null,
    val message: String? = null,
    val msg: String? = null,
    val profile: ProfileDto? = null,
    val account: AccountDto? = null,
    val data: DataDto? = null,
) {
    @Serializable
    data class DataDto(
        val code: Int? = null,
        val profile: ProfileDto? = null,
        val account: AccountDto? = null,
        val anonimousUser: Boolean? = null,
    )

    @Serializable
    data class AccountDto(
        val id: Long? = null,
        val anonimousUser: Boolean? = null,
    )

    @Serializable
    data class ProfileDto(
        val userId: Long? = null,
        val nickname: String? = null,
        val avatarUrl: String? = null,
    )

    /** 响应实际 code：顶层优先，data.code 兜底（与原项目 r.code || d.code 一致）。 */
    val actualCode: Int?
        get() = code ?: data?.code

    /** 是否匿名账号：account.anonimousUser（顶层或 data 内）。 */
    val isAnonymous: Boolean
        get() = account?.anonimousUser == true || data?.account?.anonimousUser == true
}