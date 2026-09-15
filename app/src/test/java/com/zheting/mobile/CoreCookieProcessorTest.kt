package com.zheting.mobile

import com.zheting.mobile.core.network.CookieProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreCookieProcessorTest {

    @Test
    fun extract_dropsControlKeysCaseInsensitive() {
        val raw = "MUSIC_U=abc; os=pc; Path=/; Domain=.music.163.com; Expires=Thu, 01 Jan 2030 00:00:00 GMT; HttpOnly; Secure; SameSite=Lax; __csrf=123"
        val out = CookieProcessor.extractCookie(raw)
        assertEquals("MUSIC_U=abc; os=pc; __csrf=123", out)
    }

    @Test
    fun extract_ignoresEmptyAndBareParts() {
        assertEquals("", CookieProcessor.extractCookie(""))
        assertEquals("", CookieProcessor.extractCookie("Path=/; a"))
        assertEquals("a=b", CookieProcessor.extractCookie("a=b; ; Path=/"))
    }

    @Test
    fun merge_newOverridesOldKeepsOtherKeys() {
        val merged = CookieProcessor.mergeCookies("MUSIC_U=old; __csrf=111", "MUSIC_U=new")
        assertEquals("MUSIC_U=new; __csrf=111", merged)
    }

    @Test
    fun merge_handlesEmptySides() {
        assertEquals("MUSIC_U=abc", CookieProcessor.mergeCookies("", "MUSIC_U=abc"))
        assertEquals("MUSIC_U=abc", CookieProcessor.mergeCookies("MUSIC_U=abc", ""))
    }

    @Test
    fun normalize_returnsEmptyWithoutMusicU() {
        assertEquals("", CookieProcessor.normalizeCookieForRequest("os=pc; __csrf=1"))
        assertEquals("", CookieProcessor.normalizeCookieForRequest(""))
    }

    @Test
    fun normalize_appendsOsPcWhenMissing() {
        assertEquals(
            "MUSIC_U=abc; os=pc",
            CookieProcessor.normalizeCookieForRequest("MUSIC_U=abc"),
        )
    }

    @Test
    fun normalize_keepsExistingSessionFlags() {
        assertEquals(
            "MUSIC_U=abc; os=android",
            CookieProcessor.normalizeCookieForRequest("MUSIC_U=abc;os=android"),
        )
    }

    @Test
    fun hasLoginCookie_matchesNormalizeSemantics() {
        assertTrue(CookieProcessor.hasLoginCookie("MUSIC_U=abc"))
        assertFalse(CookieProcessor.hasLoginCookie("os=pc"))
    }
}