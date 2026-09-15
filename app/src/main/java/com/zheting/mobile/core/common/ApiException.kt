package com.zheting.mobile.core.common

/**
 * 网易云信封语义的业务错误。
 *
 * 与原项目一致：HTTP 恒 200，成败看 body 里的 code。code != 200 视为失败，
 * 此处携带 code 与 message 供 UI 展示。网络/解析类错误不译为该异常。
 */
class ApiException(
    val code: Int?,
    message: String,
) : Exception(message) {
    override fun toString(): String =
        "ApiException(code=$code, message=$message)"
}