package com.zheting.mobile.data.repository

import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.network.AccountResultDto
import com.zheting.mobile.core.network.CloudsearchResponse
import com.zheting.mobile.core.network.ListResponse
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.core.network.PlaylistDetailResponse
import com.zheting.mobile.core.network.SongDetailResponse
import com.zheting.mobile.core.network.SongUrlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * songsByIds：去重、按序重建、缺失占位、code!=200 报错（契约 §2.5 / §4.5）。
 */
class SongRepositoryTest {

    private var errorCode: Int? = null

    private val fakeApi = object : NeteaseApi {
        var requestedToSingle = listOf<String>()
        override suspend fun songDetail(ids: String): SongDetailResponse {
            requestedToSingle = ids.split(",")
            if (errorCode != null) {
                return SongDetailResponse(code = errorCode, message = "bad")
            }
            return SongDetailResponse(
                code = 200,
                songs = requestedToSingle.mapNotNull { sJson(it) },
            )
        }

        override suspend fun loginStatus(timestampMs: Long, ua: String): AccountResultDto = error("unused")
        override suspend fun userAccount(timestampMs: Long): AccountResultDto = error("unused")
        override suspend fun logout(): AccountResultDto = error("unused")
        override suspend fun personalized(limit: Int): ListResponse = error("unused")
        override suspend fun personalizedNewSong(limit: Int): ListResponse = error("unused")
        override suspend fun cloudsearch(keywords: String, limit: Int, offset: Int): CloudsearchResponse = error("unused")
        override suspend fun playlistDetail(id: String): PlaylistDetailResponse = error("unused")
        override suspend fun songUrlV1(id: String, level: String, unblock: String): SongUrlResponse = error("unused")
        override suspend fun songUrlMatch(id: String): SongUrlResponse = error("unused")
        override suspend fun songUrlLegacy(id: String, br: Long): SongUrlResponse = error("unused")
    }

    private val repo = SongRepository(fakeApi)

    private fun sJson(id: String): JsonElement? {
        // /song/detail 的顶层歌曲元素；"missing" 不返回（模拟缺歌）
        if (id == "missing") return null
        return buildJsonObject {
            put("id", id)
            put("name", "song-$id")
            put("ar", buildJsonArray { add(buildJsonObject { put("id", "ar$id"); put("name", "A$id") }) })
            put("al", buildJsonObject { put("id", "al$id"); put("name", "Al$id"); put("picUrl", "http://c/$id.jpg") })
            put("dt", 200000)
        }
    }

    @Test
    fun dedup_and_keepsFirstOrder() = runTest {
        errorCode = null
        val r = repo.songsByIds(listOf("1", "1", "2"))
        val s = (r as NeteaseResult.Success).data
        assertEquals(listOf("1", "2"), s.map { it.id })
        // 只查一次（去重后再查）
        assertEquals(listOf("1", "2"), fakeApi.requestedToSingle)
    }

    @Test
    fun keepsInputOrder_and_placeholderForMissing() = runTest {
        errorCode = null
        val r = repo.songsByIds(listOf("3", "missing", "1"))
        val s = (r as NeteaseResult.Success).data
        assertEquals(listOf("3", "missing", "1"), s.map { it.id })
        val placeholder = s[1]
        assertEquals("歌曲 missing", placeholder.name)
        assertTrue(placeholder.artists.isEmpty())
        assertEquals("http://c/3.jpg", s[0].coverUrl)
    }

    @Test
    fun emptyIds_noRequest() = runTest {
        errorCode = null
        fakeApi.requestedToSingle = listOf("*")
        val r = repo.songsByIds(emptyList())
        assertTrue(r is NeteaseResult.Success)
        assertTrue((r as NeteaseResult.Success).data.isEmpty())
        assertEquals(listOf("*"), fakeApi.requestedToSingle) // 未再触发请求
    }

    @Test
    fun apiError_surfaces() = runTest {
        errorCode = -460
        val r = repo.songsByIds(listOf("1"))
        assertTrue(r is NeteaseResult.ApiError)
        val e = r as NeteaseResult.ApiError
        assertEquals(-460, e.code)
        assertEquals("bad", e.message)
    }

    @Test
    fun numericId_and_stringId_coexistAsStrings() = runTest {
        errorCode = null
        val r = repo.songsByIds(listOf("10001", "abc"))
        val s = (r as NeteaseResult.Success).data
        assertEquals(listOf("10001", "abc"), s.map { it.id })
        assertEquals(listOf("10001", "abc"), fakeApi.requestedToSingle)
    }
}