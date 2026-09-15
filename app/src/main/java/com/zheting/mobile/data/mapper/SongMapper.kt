package com.zheting.mobile.data.mapper

import com.zheting.mobile.core.model.Album
import com.zheting.mobile.core.model.Artist
import com.zheting.mobile.core.model.Song
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Song 映射（契约 §3.1，对齐原项目 normalize.ts）。
 * 元素可能是：root.song → root.resourceExtInfo.songData（→ ext.song）→ root 自身。
 * 收窄顺序：id 缺失丢弃（root 兜底时若 root 无 id 说明不是歌，返回 null）。
 */
object SongMapper {
    fun toSong(element: JsonElement): Song? {
        val root = NcmJson.asObj(element) ?: return null
        val song = routeSong(root) ?: return null
        val id = NcmJson.id(song) ?: return null

        val albumObj = (NcmJson.asObj(song["al"]) ?: NcmJson.asObj(song["album"]))
        val album = albumObj?.let { a ->
            val albumId = NcmJson.id(a)
            if (albumId == null) null else Album(
                id = albumId,
                name = NcmJson.str(a, "name"),
                coverUrl = NcmJson.str(a, "picUrl").ifEmpty { null },
            )
        }

        return Song(
            id = id,
            name = NcmJson.str(song, "name"),
            artists = (NcmJson.asArray(song["ar"]) ?: NcmJson.asArray(song["artists"]))
                ?.mapNotNull { toArtist(it) }
                .orEmpty(),
            album = album,
            durationMs = (NcmJson.long(song, "dt")).takeIf { it > 0 }
                ?: NcmJson.long(song, "duration"),
            coverUrl = listOfNotNull(
                NcmJson.asObj(song["al"])?.get("picUrl"),
                song["coverImgUrl"],
                song["picUrl"],
            ).mapNotNull { (it as? JsonPrimitive)?.content }.firstOrNull() ?: "",
        )
    }

    private fun routeSong(root: JsonObject): JsonObject? {
        // 1) root.song（多数响应）
        NcmJson.asObj(root["song"])?.let { return it }
        // 2) resourceExtInfo.songData · ext.song（个性化推荐/歌单等嵌套）
        val ext = NcmJson.asObj(root["resourceExtInfo"])
        val songData = NcmJson.asObj(ext?.get("songData"))
        if (songData != null) {
            val extObj = NcmJson.asObj(songData["ext"])
            NcmJson.asObj(extObj?.get("song"))?.let { return it }
            if (extObj != null) return extObj
            return songData
        }
        NcmJson.asObj(ext?.get("song"))?.let { return it }
        // 3) root 自身（/song/detail 的顶层歌曲元素等）
        return if (NcmJson.id(root) != null) root else null
    }

    private fun toArtist(element: JsonElement): Artist? {
        val obj = NcmJson.asObj(element) ?: return null
        val id = NcmJson.id(obj) ?: return null
        val name = NcmJson.str(obj, "name")
        if (id.isEmpty() && name.isEmpty()) return null
        return Artist(
            id = id,
            name = name,
            imageUrl = listOf("picUrl", "cover", "avatar", "img1v1Url", "img1Url")
                .firstNotNullOfOrNull { NcmJson.str(obj, it).ifEmpty { null } },
        )
    }
}