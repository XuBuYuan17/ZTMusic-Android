package com.zheting.mobile.core.model

/**
 * 领域中立模型。对应原项目 src/lib/types/music.ts。
 *
 * phase 2 规约：Song 统一使用字符串 ID（原项目 SongId = string|number，转 String 收敛）、
 * 时长统一毫秒。网易云宽松 JSON 的多态（ar/artists、al/album、dt/duration、嵌套 song）由
 * data/mapper 收窄，UI/Repository 只依赖这里的类型。
 */

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
)

data class Album(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
)

data class Song(
    val id: String,
    val name: String,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null,
    val durationMs: Long = 0,
    val coverUrl: String = "",
)

data class PlaylistRef(
    val id: String,
    val name: String,
    val coverUrl: String = "",
    val trackCount: Int = 0,
    val creatorName: String = "",
)

/** 歌单详情里的单个 trackId（用于按序补全）。原字段 id / at / addTime / time。 */
data class TrackIdRef(
    val id: String,
    val at: Long? = null,
)

data class PlaylistDetail(
    val id: String,
    val name: String,
    val coverUrl: String = "",
    val description: String = "",
    val trackCount: Int = 0,
    val creatorName: String = "",
    /** 已加载曲目（可能只是 trackIds 的一部分）。 */
    val tracks: List<Song> = emptyList(),
    /** 完整曲目序（/playlist/detail 的 trackIds），未展开前为空则不逐首补查。 */
    val trackIds: List<TrackIdRef> = emptyList(),
    /** 是否只加载了部分（trackIds 数量超过首屏上限）。 */
    val tracksPartial: Boolean = false,
)

/** 搜索歌曲结果页。 */
data class SearchPage(
    val songs: List<Song> = emptyList(),
    val songCount: Int = 0,
    val hasMore: Boolean = false,
)

/** 歌单一次「加载更多」的结果。 */
data class PlaylistChunk(
    val songs: List<Song>,
    val loadedCount: Int,
    val hasMore: Boolean,
)