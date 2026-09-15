package com.zheting.mobile.data.mapper

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Song 多态收窄单测。结构样例对齐原项目 netease.test.ts：
 * root.song / root 自身 / resourceExtInfo.songData、ar⇄artists、al⇄album、dt⇄duration。
 */
class SongMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun song(raw: String) = SongMapper.toSong(json.parseToJsonElement(raw))

    @Test
    fun rootSong_ar_dt() {
        val s = song(
            """
            {"song":{"id":5,"name":"z","ar":[{"id":6,"name":"C"}],
             "al":{"id":7,"name":"al","picUrl":"cp"},"dt":999}}
            """.trimIndent(),
        )!!
        assertEquals("5", s.id)
        assertEquals("z", s.name)
        assertEquals("C", s.artists.single().name)
        assertEquals("6", s.artists.single().id)
        assertEquals(999L, s.durationMs)
        assertEquals("cp", s.coverUrl)
        assertEquals("al", s.album?.name)
    }

    @Test
    fun rootItself_isAlbumAltVariant_durationMs() {
        // /song/detail 顶层 songs 元素 + topSongs 变体 artists/album/duration
        val s = song(
            """
            {"id":1,"name":"x","artists":[{"id":2,"name":"A"}],
             "album":{"id":3,"name":"b","picUrl":"pc"},"duration":1234,"picUrl":"/root"}
            """.trimIndent(),
        )!!
        assertEquals("1", s.id)
        assertEquals("A", s.artists.single().name)
        assertEquals(1234L, s.durationMs)
        assertEquals("/root", s.coverUrl)
    }

    @Test
    fun resourceExtInfo_songData_routesToSong() {
        val s = song(
            """
            {"resourceExtInfo":{"songData":{"id":8,"name":"q",
             "ar":[{"id":9,"name":"R"}],"al":{"id":10,"name":"alb"},"dt":3000}}}
            """.trimIndent(),
        )!!
        assertEquals("8", s.id)
        assertEquals(3000L, s.durationMs)
        assertEquals("alb", s.album?.name)
    }

    @Test
    fun resourceExtInfo_songData_ext_song() {
        val s = song(
            """
            {"resourceExtInfo":{"songData":{"ext":{"song":{"id":11,"name":"e",
             "ar":[{"id":12,"name":"E"}],"al":{"id":13,"name":"ee"}}}}}}
            """.trimIndent(),
        )!!
        assertEquals("11", s.id)
        assertEquals("E", s.artists.single().name)
    }

    @Test
    fun missingId_discarded() {
        // root 自身兜底时缺 id 视为非歌（契约 §3.1 收窄）
        assertNull(song("""{"name":"no-id"}"""))
        assertNull(song("""{}"""))
    }

    @Test
    fun numericId_becomesString() {
        assertEquals("10001", song("""{"id":10001,"name":"n"}""")?.id)
    }
}