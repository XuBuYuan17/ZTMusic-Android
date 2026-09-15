package com.zheting.mobile.data.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.parseToJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun toPlaylistRef_coverImgUrl_creatorNickname() {
        val ref = PlaylistMapper.toPlaylistRef(
            json.parseToJsonElement(
                """
                {
                  "id": "pl1", "name": "歌单", "coverImgUrl": "http://c/1.jpg",
                  "trackCount": 3, "creator": { "nickname": "N" }
                }
                """.trimIndent(),
            ),
        )!!
        assertEquals("pl1", ref.id)
        assertEquals("歌单", ref.name)
        assertEquals("http://c/1.jpg", ref.coverUrl)
        assertEquals(3, ref.trackCount)
        assertEquals("N", ref.creatorName)
    }

    @Test
    fun toPlaylistRef_picUrlFallback_creatorMissing() {
        val ref = PlaylistMapper.toPlaylistRef(
            json.parseToJsonElement("""{"id":"p2","name":"n","picUrl":"http://c/2"}"""),
        )!!
        assertEquals("http://c/2", ref.coverUrl)
        assertEquals("", ref.creatorName)
    }

    @Test
    fun toPlaylistDetail_trackIdsOrder_andPartialFlag() {
        val detail = PlaylistMapper.toPlaylistDetail(
            json.parseToJsonElement(
                """
                {
                  "id": "pl9",
                  "name": "完整",
                  "description": "desc",
                  "trackCount": 3,
                  "creator": { "nickname": "N" },
                  "tracks": [
                    { "id": 1, "name": "s1", "ar": [{"id": 10, "name": "A"}],
                      "al": { "id": 20, "name": "aL" }, "dt": 1000 }
                  ],
                  "trackIds": [
                    { "id": 1, "at": 1700000000000 },
                    { "id": 2, "addTime": 1700000000001 },
                    { "id": 3 }
                  ]
                }
                """.trimIndent(),
            ),
            fallbackId = "unused",
        )!!
        assertEquals("pl9", detail.id)
        assertEquals("desc", detail.description)
        assertEquals(3, detail.trackCount)

        assertEquals(listOf("1", "2", "3"), detail.trackIds.map { it.id })
        assertEquals(1700000000000L, detail.trackIds[0].at)
        assertEquals(1700000000001L, detail.trackIds[1].at)
        assertNull(detail.trackIds[2].at)

        assertEquals(1, detail.tracks.size)
        assertEquals("s1", detail.tracks.single().name)
        assertTrue(detail.tracksPartial)
    }

    @Test
    fun toPlaylistDetail_fullTracks_notPartial() {
        val detail = PlaylistMapper.toPlaylistDetail(
            json.parseToJsonElement(
                """
                {
                  "id": "pl0",
                  "name": "小",
                  "tracks": [{"id": 1, "name": "a"}, {"id": 2, "name": "b"}],
                  "trackIds": [{"id": 1}, {"id": 2}]
                }
                """.trimIndent(),
            ),
            fallbackId = "pl0",
        )!!
        assertFalse(detail.tracksPartial)
    }

    @Test
    fun toPlaylistDetail_noTrackIds_notPartial() {
        val detail = PlaylistMapper.toPlaylistDetail(
            json.parseToJsonElement("""{"id":"x","name":"n","trackCount":1}"""),
            fallbackId = "x",
        )!!
        assertFalse(detail.tracksPartial)
        assertTrue(detail.trackIds.isEmpty())
    }

    @Test
    fun toPlaylistDetail_missingId_usesFallback() {
        val detail = PlaylistMapper.toPlaylistDetail(
            json.parseToJsonElement("""{"name":"a"}"""),
            fallbackId = "fb",
        )!!
        assertEquals("fb", detail.id)
    }
}