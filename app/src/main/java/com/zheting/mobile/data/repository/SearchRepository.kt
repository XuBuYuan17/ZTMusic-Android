package com.zheting.mobile.data.repository

import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.common.neteaseSafe
import com.zheting.mobile.core.model.SearchPage
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.data.mapper.SongMapper

/**
 * 搜索仓库（契约 §2.3）：/cloudsearch。
 */
class SearchRepository(
    private val api: NeteaseApi,
) {
    suspend fun searchSongs(keywords: String, limit: Int = 30, offset: Int = 0): NeteaseResult<SearchPage> =
        neteaseSafe {
            val res = api.cloudsearch(keywords, limit, offset)
            if (res.code != null && res.code != 200) {
                return@neteaseSafe NeteaseResult.ApiError(res.code, res.failMessage)
            }
            val result = res.result
            if (result == null) {
                return@neteaseSafe NeteaseResult.Success(SearchPage())
            }
            NeteaseResult.Success(
                SearchPage(
                    songs = result.songs.mapNotNull(SongMapper::toSong),
                    songCount = result.songCount ?: 0,
                    hasMore = result.hasMore ?: (result.songs.size >= limit),
                ),
            )
        }
}