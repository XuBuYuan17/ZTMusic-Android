package com.zheting.mobile.data.repository

import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.common.neteaseSafe
import com.zheting.mobile.core.model.PlaylistChunk
import com.zheting.mobile.core.model.PlaylistDetail
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.data.mapper.PlaylistMapper

/**
 * 歌单仓库（契约 §2.4 / §4）。
 * detail 只拿「详情 + 首屏 tracks」；「按 trackIds 逐批补全」由 nextChunk 负责，
 * loadedCount 必须由调用方联通展示进度（部分 ≠ 完整）。
 */
class PlaylistRepository(
    private val api: NeteaseApi,
    private val songRepository: SongRepository,
) {
    suspend fun detail(id: String): NeteaseResult<PlaylistDetail> = neteaseSafe {
        val res = api.playlistDetail(id)
        if (res.code != null && res.code != 200) {
            return@neteaseSafe NeteaseResult.ApiError(res.code, res.failMessage)
        }
        val playlist = res.playlist?.let { PlaylistMapper.toPlaylistDetail(it, id) }
            ?: return@neteaseSafe NeteaseResult.ParseError(IllegalStateException("playlist 缺失"), "歌单字段缺失")
        NeteaseResult.Success(playlist)
    }

    /** 从 typeTrackIds 的 start 起取下一批（[CHUNK] 个）并补全为专辑内追加歌曲。 */
    suspend fun nextChunk(detail: PlaylistDetail, start: Int): NeteaseResult<PlaylistChunk> {
        val remaining = detail.trackIds
        if (remaining.isEmpty() || start >= remaining.size) {
            return NeteaseResult.Success(PlaylistChunk(emptyList(), start, false))
        }
        val chunkIds = remaining.drop(start).take(CHUNK)
        return when (val r = songRepository.songsByIds(chunkIds.map { it.id })) {
            is NeteaseResult.Success -> {
                // songsByIds 已按入参顺序返回（含占位），loadedCount 为已消费到的下标
                val loaded = start + chunkIds.size
                NeteaseResult.Success(
                    PlaylistChunk(r.data, loaded, loaded < remaining.size),
                )
            }
            // 失败类型是 NeteaseResult<Nothing>（协变），逐条显式回收避免整体类型发钝为 Any
            is NeteaseResult.ApiError -> NeteaseResult.ApiError(r.code, r.message)
            is NeteaseResult.NetworkError -> NeteaseResult.NetworkError(r.cause)
            is NeteaseResult.ParseError -> NeteaseResult.ParseError(r.cause, r.message)
        }
    }

    companion object {
        /** 每次 song/detail 补全的 ids 数量（契约 §4.2，50 便于边载边展示）。 */
        const val CHUNK = 50
    }
}