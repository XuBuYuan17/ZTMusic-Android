package com.zheting.mobile.data.mapper

import com.zheting.mobile.core.model.AuthUser
import com.zheting.mobile.core.network.AccountResultDto

/**
 * 登录态响应 → 领域模型。字段兜底顺序对齐原项目 stores/auth.svelte.ts 的 normalizeUser。
 */
fun AccountResultDto.toAuthUser(): AuthUser? {
    val topProfile = profile
    val dataProfile = data?.profile
    val accountId = account?.id ?: data?.account?.id

    val userIdV = (topProfile?.userId ?: dataProfile?.userId ?: accountId) ?: return null
    val nickname = topProfile?.nickname ?: dataProfile?.nickname ?: "用户"
    val avatarUrl = topProfile?.avatarUrl ?: dataProfile?.avatarUrl ?: ""
    return AuthUser(
        userId = userIdV.toString(),
        nickname = nickname,
        avatarUrl = avatarUrl,
    )
}