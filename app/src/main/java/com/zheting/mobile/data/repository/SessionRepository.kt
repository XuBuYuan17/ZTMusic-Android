package com.zheting.mobile.data.repository

import com.zheting.mobile.core.model.AuthUser
import com.zheting.mobile.core.model.SessionStatus
import com.zheting.mobile.core.network.AccountResultDto
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.core.network.SessionManager
import com.zheting.mobile.data.mapper.toAuthUser
import kotlinx.coroutines.CancellationException

/**
 * 会话链路仓库：登录态校验 / 登出。
 *
 * - UI 不直接触网，只消费本仓库与领域模型；
 * - 网络层只负责传输，code/登录态语义在这里判定并翻译成 SessionStatus。
 */
class SessionRepository(
    private val api: NeteaseApi,
    private val session: SessionManager,
) {
    /** 当前 API base，供 UI 展示（只读）。 */
    val apiBase: String get() = session.getBase()

    /**
     * 会话自检：无有效 cookie → NotLoggedIn；
     * 有 cookie 但服务端判定匿名/失败 → 清空 cookie 并回 NotLoggedIn；
     * 请求/解析异常 → Error。
     */
    suspend fun checkLoginStatus(): SessionStatus {
        if (!session.hasLoginCookie()) return SessionStatus.NotLoggedIn
        return try {
            val res = api.loginStatus(System.currentTimeMillis())
            if (res.actualCode != 200 || res.isAnonymous) {
                session.clearCookie()
                SessionStatus.NotLoggedIn
            } else {
                resolveUser(res) ?: SessionStatus.NotLoggedIn
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SessionStatus.Error(e)
        }
    }

    /** 先 /user/account 兜底取 profile，与原项目 qrLogin 一致。 */
    private suspend fun resolveUser(loginStatus: AccountResultDto): AuthUser? {
        loginStatus.toAuthUser()?.let { return it }
        return try {
            api.userAccount(System.currentTimeMillis()).toAuthUser()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}