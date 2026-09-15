package com.zheting.mobile.core.common

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Repository 统一出口（契约 §5）。
 * - 空列表也是 [Success]，与错误区分。
 * - 协程取消重新抛出，不落入任何分支。
 */
sealed interface NeteaseResult<out T> {
    data class Success<T>(val data: T) : NeteaseResult<T>

    /** body.code != 200（或登录态异返回码）；message 取 message → msg。 */
    data class ApiError(val code: Int, val message: String) : NeteaseResult<Nothing>

    /** IOException / 超时 / HTTP 层异常（非业务失败）。 */
    data class NetworkError(val cause: Throwable) : NeteaseResult<Nothing>

    /** 响应结构解析失败（序列化异常）。 */
    data class ParseError(val cause: Throwable, val message: String) : NeteaseResult<Nothing>
}

/**
 * 把一次 repository 网络调用翻译成 [NeteaseResult]。
 * 只有 IOException 与序列化异常被收敛；Cancel 向上传播。
 */
suspend inline fun <T> neteaseSafe(crossinline block: suspend () -> NeteaseResult<T>): NeteaseResult<T> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        NeteaseResult.NetworkError(e)
    } catch (e: SerializationException) {
        NeteaseResult.ParseError(e, e.message ?: "响应解析失败")
    } catch (e: Exception) {
        // 其他意外（含 retrofit 转换层）；保留原始错误，避免吞掉
        NeteaseResult.NetworkError(e)
    }