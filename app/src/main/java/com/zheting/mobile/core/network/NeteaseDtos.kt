package com.zheting.mobile.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 端点 DTO（契约 §2）。信封字段在各响应顶层，元素保留 [JsonElement]
 * 由 data/mapper 收窄多态（对齐原项目 normalize.ts）。
 *
 * 注意不能假设统一 {code, data}：/song/detail 的歌曲在顶层 songs，
 * /playlist/detail 的歌单在顶层 playlist，/search 系在 result。
 */

/** { code, message|msg?, result: [...] } —— /personalized 与 /personalized/newsong。 */
@Serializable
data class ListResponse(
    val code: Int? = null,
    val message: String? = null,
    val msg: String? = null,
    val result: List<JsonElement> = emptyList(),
) {
    val failMessage: String get() = message ?: msg ?: ""
}

/** { code, result: { songs, songCount?, hasMore? } } —— /cloudsearch。 */
@Serializable
data class CloudsearchResponse(
    val code: Int? = null,
    val message: String? = null,
    val msg: String? = null,
    val result: SearchResult? = null,
) {
    val failMessage: String get() = message ?: msg ?: ""

    @Serializable
    data class SearchResult(
        val songs: List<JsonElement> = emptyList(),
        val songCount: Int? = null,
        val hasMore: Boolean? = null,
    )
}

/** { code, songs: [...] } —— /song/detail。 */
@Serializable
data class SongDetailResponse(
    val code: Int? = null,
    val message: String? = null,
    val msg: String? = null,
    val songs: List<JsonElement> = emptyList(),
) {
    val failMessage: String get() = message ?: msg ?: ""
}

/** { code, playlist: {...} } —— /playlist/detail。 */
@Serializable
data class PlaylistDetailResponse(
    val code: Int? = null,
    val message: String? = null,
    val msg: String? = null,
    val playlist: JsonElement? = null,
) {
    val failMessage: String get() = message ?: msg ?: ""
}

/** /song/url/v1、/song/url/match、/song/url（旧）共用信封：{ code, data:[...] }，match 可能顶层 url。 */
@Serializable
data class SongUrlResponse(
    val code: Int? = null,
    val message: String? = null,
    val msg: String? = null,
    /** match/旧接口个别返回顶层 url。 */
    val url: String? = null,
    val data: List<SongUrlItem>? = null,
) {
    val failMessage: String get() = message ?: msg ?: ""

    /** 首条候选 url：data[0].url → 顶层 url（对齐原 provider 的取法顺序）。 */
    fun firstPlayUrl(): String? =
        data?.firstNotNullOfOrNull { it.url?.takeIf(String::isNotBlank) }
            ?: url?.takeIf(String::isNotBlank)

    /** 首个候选的完整信息（url + 试听标记）。data[0] 优先。 */
    fun firstPlayable(): SongUrlItem? = data?.firstOrNull()

    @Serializable
    data class SongUrlItem(
        val code: Int? = null,
        val url: String? = null,
        val message: String? = null,
        val msg: String? = null,
        /** 存在即试听片段（对齐原 getStream 的 freeTrialInfo 判定）。 */
        val freeTrialInfo: JsonElement? = null,
    ) {
        val isTrial: Boolean get() = freeTrialInfo != null
    }
}