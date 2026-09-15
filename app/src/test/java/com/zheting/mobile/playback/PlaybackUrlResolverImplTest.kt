package com.zheting.mobile.playback

import com.zheting.mobile.core.model.Song
import com.zheting.mobile.core.network.SongUrlEndpoints
import com.zheting.mobile.core.network.SongUrlResponse
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 候选链单测：对照原项目 url-resolver.ts 的 Phase 顺序。
 * fake 只实现三个 URL 端点（窄接口注入）。
 */
class PlaybackUrlResolverImplTest {

    private val song = Song(id = "777", name = "歌", durationMs = 0)

    private class FakeEndpoints : SongUrlEndpoints {
        /** 记录每次 v1 调用的 (level, unblock)。 */
        val v1Calls = mutableListOf<Pair<String, String>>()
        var v1Handler: (String, String) -> SongUrlResponse = { _, _ -> SongUrlResponse(data = null) }
        var matchHandler: (String) -> SongUrlResponse = { SongUrlResponse(data = null) }
        var legacyHandler: (String, Long) -> SongUrlResponse = { _, _ -> SongUrlResponse(data = null) }

        override suspend fun songUrlV1(id: String, level: String, unblock: String): SongUrlResponse {
            v1Calls += level to unblock
            return v1Handler(level, unblock)
        }

        override suspend fun songUrlMatch(id: String): SongUrlResponse = matchHandler(id)

        override suspend fun songUrlLegacy(id: String, br: Long): SongUrlResponse = legacyHandler(id, br)
    }

    private fun item(url: String, trial: Boolean = false) = SongUrlResponse.SongUrlItem(
        url = url,
        freeTrialInfo = if (trial) JsonPrimitive(true) else null,
    )

    @Test
    fun `Phase1_standard非试听直接中标`() {
        val ep = FakeEndpoints().apply {
            v1Handler = { _, _ -> SongUrlResponse(data = listOf(item("http://std")) ) }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("http://std"), urls.map { it.url })
        assertTrue(urls.none { it.isTrial })
        // 首个非试听即 break：standard 命中后不应再试 higher
        assertEquals(1, ep.v1Calls.size)
        assertEquals("standard" to "false", ep.v1Calls[0])
    }

    @Test
    fun `Phase1_更高音质级别按顺序回退`() {
        val ep = FakeEndpoints().apply {
            v1Handler = { level, _ ->
                if (level == "standard") SongUrlResponse(data = listOf(item("http://std", trial = true)))
                else SongUrlResponse(data = listOf(item("http://higher")))
            }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("http://higher"), urls.map { it.url })
    }

    @Test
    fun `Phase2_普通全无音源时unblock补充`() {
        val ep = FakeEndpoints().apply {
            v1Handler = { level, unblock ->
                if (unblock == "true") SongUrlResponse(data = listOf(item("http://unblock")))
                else SongUrlResponse(data = null)
            }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("http://unblock"), urls.map { it.url })
        assertTrue(ep.v1Calls.any { (_, unblock) -> unblock == "true" })
    }

    @Test
    fun `Phase3_v1两遍都为空走match`() {
        val ep = FakeEndpoints().apply {
            matchHandler = { SongUrlResponse(data = listOf(item("http://match"), item("http://match2"))) }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("http://match"), urls.map { it.url })
    }

    @Test
    fun `Phase4_match为空走旧接口`() {
        val ep = FakeEndpoints().apply {
            legacyHandler = { _, br -> SongUrlResponse(data = listOf(item("http://legacy"))) }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("http://legacy"), urls.map { it.url })
    }

    @Test
    fun `Phase5_只有试听片段时用试听兜底`() {
        val ep = FakeEndpoints().apply {
            v1Handler = { level, _ -> SongUrlResponse(data = listOf(item("http://trial-$level", trial = true))) }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("http://trial-standard", "http://trial-higher"), urls.map { it.url })
        assertTrue(urls.all { it.isTrial })
    }

    @Test
    fun `Phase6_全链无结果用官方外链模板`() {
        val ep = FakeEndpoints()
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("https://music.163.com/song/media/outer/url?id=777.mp3"), urls.map { it.url })
    }

    @Test
    fun `试听候选去重后按收集顺序入列`() {
        val ep = FakeEndpoints().apply {
            v1Handler = { level, _ ->
                if (level == "standard") SongUrlResponse(data = listOf(item("http://t", trial = true)))
                else SongUrlResponse(data = listOf(item("http://t", trial = true))) // 与 standard 相同 url
            }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("http://t"), urls.map { it.url })
    }

    @Test
    fun `music_126_net的http统一升级为https`() {
        val ep = FakeEndpoints().apply {
            v1Handler = { _, _ -> SongUrlResponse(data = listOf(item("http://m9.music.126.net/abc"))) }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        assertEquals(listOf("https://m9.music.126.net/abc"), urls.map { it.url })
    }

    @Test
    fun `空字符串url视为无结果`() {
        val ep = FakeEndpoints().apply {
            matchHandler = { SongUrlResponse(data = listOf(item(""))) }
        }
        val urls = PlaybackUrlResolverImpl(ep).resolve(song)

        // match 只有空串 → 走 Phase4 → Phase6 模板兜底
        assertEquals(listOf("https://music.163.com/song/media/outer/url?id=777.mp3"), urls.map { it.url })
    }
}