package com.zheting.mobile.data.repository

import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.common.neteaseSafe
import com.zheting.mobile.core.model.PlaylistRef
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.data.mapper.PlaylistMapper
import com.zheting.mobile.data.mapper.SongMapper

/**
 * 首页仓库（契约 §2.1 / §2.2）：推荐歌单 + 推荐新歌。
 * 空 result 视为 Success + 空列表（契约 §1.2）。
 */
class HomeRepository(
    private val api: NeteaseApi,
) {
    suspend fun personalized(limit: Int = 10): NeteaseResult<List<PlaylistRef>> = neteaseSafe {
        val res = api.personalized(limit)
        if (res.code != null && res.code != 200) {
            return@neteaseSafe NeteaseResult.ApiError(res.code, res.failMessage)
        }
        NeteaseResult.Success(res.result.mapNotNull(PlaylistMapper::toPlaylistRef))
    }

    suspend fun newSongs(limit: Int = 12): NeteaseResult<List<Song>> = neteaseSafe {
        val res = api.personalizedNewSong(limit)
        if (res.code != null && res.code != 200) {
            return@neteaseSafe NeteaseResult.ApiError(res.code, res.failMessage)
        }
        NeteaseResult.Success(res.result.mapNotNull(SongMapper::toSong))
    }
}