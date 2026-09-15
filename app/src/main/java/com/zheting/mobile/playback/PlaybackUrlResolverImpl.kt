package com.zheting.mobile.playback

import com.zheting.mobile.core.model.Song
import com.zheting.mobile.core.network.SongUrlEndpoints
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 可播 URL 解析（对齐原项目 url-resolver.ts getPlayableUrls 的候选链）。
 *
 * Phase 1  快速出声：standard→higher→用户偏好，普通接口，首个非试听即中（试听先攒着）；
 * Phase 2  unblock：普通接口带 unblock=true 再扫一遍（同样非试听优先）；
 * Phase 3  /song/url/match（灰色直连）；
 * Phase 4  /song/url（旧接口，br=320000）；
 * Phase 5  前面攒下的试听候选兜上；
 * Phase 6  官方外链模板兜底。
 *
 * 每一级只有前一级「无任何候选」才进入；试听判定 freeTrialInfo 非空。
 * 单次调用超时按原 FAST_TIMEOUT=3500ms 收敛，超时/异常视为该级无结果；
 * 外层协程取消不会被吞（CancellationException 原样上抛）。
 */
class PlaybackUrlResolverImpl(
    private val endpoints: SongUrlEndpoints,
    private val preferredLevel: String = DEFAULT_PREFERRED_LEVEL,
) : PlaybackUrlResolver {

    override suspend fun resolve(song: Song): List<AudioCandidate> {
        val id = song.id
        val fastTiers = listOf("standard", "higher", preferredLevel).distinct()

        val candidates = mutableListOf<AudioCandidate>()
        val trials = mutableListOf<AudioCandidate>()

        // Phase 1：普通接口快速出声（非试听优先）
        for (level in fastTiers) {
            val item = uriCall(FAST_TIMEOUT_MS) { endpoints.songUrlV1(id, level, UNBLOCK_FALSE) }
                ?.firstPlayable() ?: continue
            val url = normalizeUrl(item.url) ?: continue
            if (item.isTrial) {
                trials.addCandidateOnce(AudioCandidate(url, isTrial = true))
                continue
            }
            candidates.addOnce(AudioCandidate(url))
            break
        }

        // Phase 2：unblock=true 再扫
        if (candidates.isEmpty()) {
            for (level in fastTiers) {
                val item = uriCall(FAST_TIMEOUT_MS) { endpoints.songUrlV1(id, level, UNBLOCK_TRUE) }
                    ?.firstPlayable() ?: continue
                val url = normalizeUrl(item.url) ?: continue
                if (item.isTrial) {
                    trials.addCandidateOnce(AudioCandidate(url, isTrial = true))
                    continue
                }
                candidates.addOnce(AudioCandidate(url))
                break
            }
        }

        // Phase 3：官方 match 解灰
        if (candidates.isEmpty()) {
            uriCall(FAST_TIMEOUT_MS) { endpoints.songUrlMatch(id) }
                ?.let { resp -> normalizeUrl(resp.firstPlayUrl()) }
                ?.let { candidates.addOnce(AudioCandidate(it)) }
        }

        // Phase 4：旧 /song/url 兜底
        if (candidates.isEmpty()) {
            uriCall(FAST_TIMEOUT_MS) { endpoints.songUrlLegacy(id, LEGACY_BR) }
                ?.let { resp -> normalizeUrl(resp.firstPlayUrl()) }
                ?.let { candidates.addOnce(AudioCandidate(it)) }
        }

        // Phase 5：已收集的试听候选
        if (candidates.isEmpty() && trials.isNotEmpty()) {
            candidates.addAll(trials) // trials 内部已去重
        }

        // Phase 6：官方外链模板兜底
        if (candidates.isEmpty()) {
            candidates.add(AudioCandidate(fallbackTemplate(id)))
        }

        return candidates
    }

    private fun <T> uriCall(timeoutMs: Long, block: suspend () -> T): T? =
        withTimeoutOrNull(timeoutMs) { block() }

    /** http://*.music.126.net 统一升级为 https，其余原样 trim 返回。 */
    internal fun normalizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val t = url.trim()
        val lower = t.lowercase()
        if (lower.startsWith("http://") && lower.contains(".music.126.net")) {
            return "https://" + t.substringAfter("http://")
        }
        return t
    }

    companion object {
        /** 对齐 constants.ts QUALITY_ORDER 中最常见的默认值。 */
        const val DEFAULT_PREFERRED_LEVEL = "standard"
        const val LEGACY_BR = 320000L
        const val UNBLOCK_FALSE = "false"
        const val UNBLOCK_TRUE = "true"
        /** 对齐 PLAYBACK.FAST_TIMEOUT (ms)。 */
        const val FAST_TIMEOUT_MS = 3500L

        /** 网易云音乐官方 fallback URL 模板（constants.ts FALLBACK_URL_TEMPLATE）。 */
        internal fun fallbackTemplate(id: String): String =
            "https://music.163.com/song/media/outer/url?id=$id.mp3"
    }
}

private fun MutableList<AudioCandidate>.addOnce(candidate: AudioCandidate) {
    if (none { it.url == candidate.url }) add(candidate)
}

private fun MutableList<AudioCandidate>.addCandidateOnce(candidate: AudioCandidate) = addOnce(candidate)