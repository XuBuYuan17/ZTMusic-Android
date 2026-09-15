package com.zheting.mobile.data.repository

import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.network.AccountResultDto
import com.zheting.mobile.core.network.CloudsearchResponse
import com.zheting.mobile.core.network.ListResponse
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.core.network.PlaylistDetailResponse
import com.zheting.mobile.core.network.SongDetailResponse
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Home/Search 判定：code!=200 → ApiError；空 result → Success + 空列表（契约 §1.2）。
 */
class HomeSearchRepositoryTest {

    private var listErrorCode: Int? = null

    private val fakeApi = object : NeteaseApi {
        override suspend fun personalized(limit: Int): ListResponse {
            if (listErrorCode != null) return ListResponse(code = listErrorCode, message = "bad")
            return ListResponse(
                code = 200,
                result = buildJsonArray {
                    add(buildJsonObject { put("id", "p1"); put("name", "推荐1"); put("trackCount", 3) })
                    add(buildJsonObject { put("id", "p2"); put("name", "推荐2") })
                },
            )
        }

        override suspend fun personalizedNewSong(limit: Int): ListResponse {
            return ListResponse(
                code = 200,
                result = buildJsonArray {
                    add(buildJsonObject { put("id", 1001); put("name", "新歌") })
                },
            )
        }

        override suspend fun cloudsearch(keywords: String, limit: Int, offset: Int): CloudsearchResponse {
            if (listErrorCode != null) return CloudsearchResponse(code = listErrorCode, message = "bad")
            return CloudsearchResponse(
                code = 200,
                result = CloudsearchResponse.SearchResult(
                    songs = buildJsonArray {
                        add(buildJsonObject { put("id", 1); put("name", "b") })
                        add(buildJsonObject { put("id", "a"); put("name", "c") })
                    },
                    songCount = 2,
                    hasMore = false,
                ),
            )
        }

        override suspend fun loginStatus(timestampMs: Long, ua: String): AccountResultDto = error("unused")
        override suspend fun userAccount(timestampMs: Long): AccountResultDto = error("unused")
        override suspend fun logout(): AccountResultDto = error("unused")
        override suspend fun playlistDetail(id: String): PlaylistDetailResponse = error("unused")
        override suspend fun songDetail(ids: String): SongDetailResponse = error("unused")
        override suspend fun songUrlV1(id: String, level: String, unblock: String) = error("unused")
        override suspend fun songUrlMatch(id: String) = error("unused")
        override suspend fun songUrlLegacy(id: String, br: Long) = error("unused")
    }

    private val home = HomeRepository(fakeApi)
    private val search = SearchRepository(fakeApi)

    @Test
    fun personalized_parsesAndEmptyConfirmed() {
        listErrorCode = null
        val r = home.personalized(limit = 2)
        val refs = (r as NeteaseResult.Success).data
        assertEquals(listOf("p1", "p2"), refs.map { it.id })
        assertEquals("推荐1", refs[0].name)
    }

    @Test
    fun personalized_apiError() {
        listErrorCode = -460
        val r = home.personalized(limit = 2)
        assertTrue(r is NeteaseResult.ApiError)
        assertEquals(-460, (r as NeteaseResult.ApiError).code)
    }

    @Test
    fun newSongs_parsesAndUnifiesIdToString() {
        val r = home.newSongs(limit = 1)
        val songs = (r as NeteaseResult.Success).data
        assertEquals("1001", songs.single().id)
        assertEquals("新歌", songs.single().name)
    }

    @Test
    fun search_resultNull_isSuccessEmpty() {
        val fake2 = object : NeteaseApi {
            override suspend fun cloudsearch(keywords: String, limit: Int, offset: Int): CloudsearchResponse =
                CloudsearchResponse(code = 200, result = null)
            // 其余未用
            override suspend fun loginStatus(timestampMs: Long, ua: String) = error("unused")
            override suspend fun userAccount(timestampMs: Long) = error("unused")
            override suspend fun logout() = error("unused")
            override suspend fun personalized(limit: Int) = error("unused")
            override suspend fun personalizedNewSong(limit: Int) = error("unused")
            override suspend fun playlistDetail(id: String) = error("unused")
            override suspend fun songDetail(ids: String) = error("unused")
            override suspend fun songUrlV1(id: String, level: String, unblock: String) = error("unused")
            override suspend fun songUrlMatch(id: String) = error("unused")
            override suspend fun songUrlLegacy(id: String, br: Long) = error("unused")
        }
        val r = SearchRepository(fake2).searchSongs("x")
        val page = (r as NeteaseResult.Success).data
        assertTrue(page.songs.isEmpty())
        assertEquals(0, page.songCount)
    }

    @Test
    fun search_parsesSongs_andMeta() {
        listErrorCode = null
        val r = search.searchSongs("关键词")
        val page = (r as NeteaseResult.Success).data
        assertEquals(listOf("1", "a"), page.songs.map { it.id })
        assertEquals(2, page.songCount)
    }

    @Test
    fun search_apiError() {
        listErrorCode = -460
        val r = search.searchSongs("x")
        assertTrue(r is NeteaseResult.ApiError)
    }
}