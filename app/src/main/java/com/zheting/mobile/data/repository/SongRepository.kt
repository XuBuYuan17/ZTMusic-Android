package com.zheting.mobile.data.repository

import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.common.neteaseSafe
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.data.mapper.SongMapper

/**
 * 歌曲仓库（契约 §2.5）：批量 /song/detail。
 * - 入参去重后再查，结果回填去重后仍按入参顺序。
 * - 单次批量上限 [SONG_DETAIL_BATCH_SIZE]。
 */
class SongRepository(
    private val api: NeteaseApi,
) {
    suspend fun songsByIds(ids: List<String>): NeteaseResult<List<Song>> {
        val unique = ids.distinct()
        if (unique.isEmpty()) return NeteaseResult.Success(emptyList())
        return neteaseSafe {
            val index = unique.withIndex().associate { it.value to it.index }
            val ordered = Array<Song?>(unique.size) { null }
            for (chunk in unique.chunked(SONG_DETAIL_BATCH_SIZE)) {
                val res = api.songDetail(chunk.joinToString(","))
                if (res.code != null && res.code != 200) {
                    return@neteaseSafe NeteaseResult.ApiError(res.code, res.failMessage)
                }
                for (element in res.songs) {
                    val song = SongMapper.toSong(element) ?: continue
                    index[song.id]?.let { ordered[it] = song }
                }
            }
            // 占位缺失 id，保持序号完整（契约 §4.3）
            val filled = unique.mapIndexed { i, id -> ordered[i] ?: Song(id = id, name = "歌曲 $id") }
            NeteaseResult.Success(filled)
        }
    }

    companion object {
        const val SONG_DETAIL_BATCH_SIZE = 500
    }
}