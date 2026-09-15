package com.zheting.mobile.core.network

/**
 * 播放 URL 解析所需的最小端点集合（实现在 [NeteaseApi] 上）。
 * 单独成接口便于 resolver 单测注入窄 fake，不必实现全部业务端点。
 */
interface SongUrlEndpoints {

    /** /song/url/v1 —— 普通/解锁接口，level 原样透传，unblock "true"/"false"。 */
    suspend fun songUrlV1(id: String, level: String, unblock: String): SongUrlResponse

    /** /song/url/match —— 灰色歌曲直连。 */
    suspend fun songUrlMatch(id: String): SongUrlResponse

    /** /song/url（旧）—— 老接口按码率 br 兜底。 */
    suspend fun songUrlLegacy(id: String, br: Long): SongUrlResponse
}