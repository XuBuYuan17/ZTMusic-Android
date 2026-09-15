package com.zheting.mobile.data.repository

import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.model.PlaylistDetail
import com.zheting.mobile.core.network.AccountResultDto
import com.zheting.mobile.core.network.CloudsearchResponse
import com.zheting.mobile.core.network.ListResponse
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.core.network.PlaylistDetailResponse
import com.zheting.mobile.core.network.SongDetailResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 歌单补全协议（契约 §4）：nextChunk 按 trackIds 切片、每批 50、占位补齐、
 * loaded/hasMore 联通进度；detail 走重定向失败路径。
 */
class PlaylistRepositoryTest {

    private var detailErrorCode: Int? = null
    private var trackIdsTotal = 0
    private var loadedTracks = 0

    private val fakeApi = object : NeteaseApi {
        var lastSongDetailIds = listOf<String>()

        override suspend fun playlistDetail(id: String): PlaylistDetailResponse {
            if (detailErrorCode != null) {
                return PlaylistDetailResponse(code = detailErrorCode, message = "bad")
            }
            return PlaylistDetailResponse(
                code = 200,
                playlist = buildJsonObject {
                    put("id", id)
                    put("name", "歌单$id")
                    put("trackCount", trackIdsTotal)
                    put(
                        "tracks",
                        buildJsonArray {
                            repeat(loadedTracks) { i ->
                                add(sJson((i + 1).toString())!!)
                            }
                        },
                    )
                    put(
                        "trackIds",
                        buildJsonArray {
                            repeat(trackIdsTotal) { i ->
                                add(buildJsonObject { put("id", (i + 1).toString()) })
                            }
                        },
                    )
                },
            )
        }

        override suspend fun songDetail(ids: String): SongDetailResponse {
            lastSongDetailIds = ids.split(",")
            return SongDetailResponse(
                code = 200,
                songs = lastSongDetailIds.mapNotNull { sJson(it) },
            )
        }

        override suspend fun loginStatus(timestampMs: Long, ua: String): AccountResultDto = error("unused")
        override suspend fun userAccount(timestampMs: Long): AccountResultDto = error("unused")
        override suspend fun logout(): AccountResultDto = error("unused")
        override suspend fun personalized(limit: Int): ListResponse = error("unused")
        override suspend fun personalizedNewSong(limit: Int): ListResponse = error("unused")
        override suspend fun cloudsearch(keywords: String, limit: Int, offset: Int): CloudsearchResponse = error("unused")
        override suspend fun songUrlV1(id: String, level: String, unblock: String) = error("unused")
        override suspend fun songUrlMatch(id: String) = error("unused")
        override suspend fun songUrlLegacy(id: String, br: Long) = error("unused")
    }

    private val repo = PlaylistRepository(fakeApi, SongRepository(fakeApi))

    private fun sJson(id: String): JsonElement? =
        if (id == "missing") null else buildJsonObject {
            put("id", id)
            put("name", "song-$id")
        }

    private suspend fun goodDetail(): PlaylistDetail {
        detailErrorCode = null // 各用例互不影响（JUnit 不保证执行顺序）
        trackIdsTotal = 200
        loadedTracks = 1 // 首屏只带回 1 首（典型 partial）
        return (repo.detail("pl9") as NeteaseResult.Success).data
    }

    @Test
    fun detail_partial_matchesContractS4() = runTest {
        val d = goodDetail()
        assertEquals("pl9", d.id)
        assertEquals(200, d.trackIds.size)
        assertEquals(1, d.tracks.size)
        assertTrue(d.tracksPartial)
    }

    @Test
    fun nextChunk_respectsStartAndBatches50() = runTest {
        val d = goodDetail()
        // 首屏已加载 1 首，从下标 1 续补
        val r = repo.nextChunk(d, start = 1)
        val chunk = (r as NeteaseResult.Success).data
        assertEquals(50, chunk.songs.size)
        assertEquals(51, chunk.loadedCount)
        assertTrue(chunk.hasMore)
        assertEquals((2..51).map { it.toString() }, chunk.songs.map { it.id })
    }

    @Test
    fun nextChunk_lastBatch_hasMoreFalse() = runTest {
        val d = goodDetail()
        // 模拟已 load 到 150，最后一批 50 → 200
        val r = repo.nextChunk(d, start = 150)
        val chunk = (r as NeteaseResult.Success).data
        assertEquals(50, chunk.songs.size)
        assertEquals(200, chunk.loadedCount)
        assertFalse(chunk.hasMore)
        assertEquals((151..200).map { it.toString() }, chunk.songs.map { it.id })
    }

    @Test
    fun nextChunk_missing_tracksBecomePlaceholders() = runTest {
        detailErrorCode = null
        trackIdsTotal = 3
        loadedTracks = 0
        val d = (repo.detail("plm") as NeteaseResult.Success).data
        val withMissing = d.copy(
            trackIds = listOf("1", "missing", "3").map { com.zheting.mobile.core.model.TrackIdRef(it) },
        )
        val chunk = (repo.nextChunk(withMissing, start = 0) as NeteaseResult.Success).data
        assertEquals(listOf("1", "missing", "3"), chunk.songs.map { it.id })
        assertEquals("歌曲 missing", chunk.songs[1].name)
        assertTrue(chunk.songs[1].artists.isEmpty())
        assertEquals(3, chunk.loadedCount)
        assertFalse(chunk.hasMore)
    }

    @Test
    fun nextChunk_emptyTrackIds_returnsEmpty() = runTest {
        detailErrorCode = null
        val d = (repo.detail("ple") as NeteaseResult.Success).data.copy(trackIds = emptyList())
        val chunk = (repo.nextChunk(d, start = 0) as NeteaseResult.Success).data
        assertTrue(chunk.songs.isEmpty())
        assertEquals(0, chunk.loadedCount)
        assertFalse(chunk.hasMore)
    }

    @Test
    fun detail_codeNot200_apiError() = runTest {
        detailErrorCode = -460
        val r = repo.detail("pl9")
        assertTrue(r is NeteaseResult.ApiError)
        assertEquals(-460, (r as NeteaseResult.ApiError).code)
    }
}