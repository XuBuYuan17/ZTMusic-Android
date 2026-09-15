package com.zheting.mobile.data.mapper

import com.zheting.mobile.core.model.PlaylistDetail
import com.zheting.mobile.core.model.PlaylistRef
import com.zheting.mobile.core.model.TrackIdRef
import kotlinx.serialization.json.JsonElement

/**
 * Playlist 映射（契约 §3.2）。coverUrl = coverImgUrl → picUrl，creator 缺省空串。
 * 详情补齐协议（§4）由 data/repository/PlaylistRepository 负责。
 */
object PlaylistMapper {
    fun toPlaylistRef(element: JsonElement): PlaylistRef? {
        val obj = NcmJson.asObj(element) ?: return null
        val id = NcmJson.id(obj) ?: return null
        return PlaylistRef(
            id = id,
            name = NcmJson.str(obj, "name"),
            coverUrl = playlistCover(obj),
            trackCount = NcmJson.int(obj, "trackCount"),
            creatorName = NcmJson.str(NcmJson.asObj(obj["creator"]), "nickname"),
        )
    }

    /** 歌单详情对象（/playlist/detail 的顶层 playlist）。 */
    fun toPlaylistDetail(element: JsonElement, fallbackId: String): PlaylistDetail? {
        val obj = NcmJson.asObj(element) ?: return null
        val id = NcmJson.id(obj) ?: fallbackId
        val trackIds = NcmJson.asArray(obj["trackIds"])
            ?.mapNotNull {
                val t = NcmJson.asObj(it) ?: return@mapNotNull null
                val tid = NcmJson.id(t) ?: NcmJson.str(t, "id").takeIf(String::isNotEmpty) ?: return@mapNotNull null
                TrackIdRef(
                    id = tid,
                    at = NcmJson.long(t, "at").takeIf { it > 0 }
                        ?: NcmJson.long(t, "addTime").takeIf { it > 0 },
                )
            } ?: emptyList()

        val tracks = NcmJson.asArray(obj["tracks"])?.mapNotNull(SongMapper::toSong) ?: emptyList()

        return PlaylistDetail(
            id = id,
            name = NcmJson.str(obj, "name"),
            coverUrl = playlistCover(obj),
            description = NcmJson.str(obj, "description"),
            trackCount = NcmJson.int(obj, "trackCount"),
            creatorName = NcmJson.str(NcmJson.asObj(obj["creator"]), "nickname"),
            tracks = tracks,
            trackIds = trackIds,
            tracksPartial = trackIds.isNotEmpty() && tracks.isNotEmpty() && tracks.size < trackIds.size,
        )
    }

    private fun playlistCover(obj: kotlinx.serialization.json.JsonObject): String =
        NcmJson.str(obj, "coverImgUrl").ifEmpty { NcmJson.str(obj, "picUrl") }
}