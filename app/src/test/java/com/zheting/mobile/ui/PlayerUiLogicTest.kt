package com.zheting.mobile.ui

import com.zheting.mobile.feature.player.LyricLine
import com.zheting.mobile.feature.player.activeLyricIndex
import com.zheting.mobile.feature.player.shouldDismissPlayer
import org.junit.Assert.*
import org.junit.Test

class PlayerUiLogicTest {
    @Test fun dismissalRequiresDistanceOrDeliberateFling() {
        assertFalse(shouldDismissPlayer(10f, 800f, 2000f))
        assertFalse(shouldDismissPlayer(80f, 800f, -1000f))
        assertTrue(shouldDismissPlayer(170f, 800f, 0f))
        assertTrue(shouldDismissPlayer(30f, 800f, 1200f))
        assertFalse(shouldDismissPlayer(90f, 300f, 0f))
        assertTrue(shouldDismissPlayer(100f, 300f, 0f))
    }

    @Test fun lyricSelectionHandlesBoundariesDuplicatesAndBackwardSeek() {
        val lines = listOf(LyricLine(1000, "a"), LyricLine(2000, "b"), LyricLine(2000, "c"), LyricLine(5000, "d"))
        assertEquals(-1, activeLyricIndex(emptyList(), 100))
        assertEquals(-1, activeLyricIndex(lines, 0))
        assertEquals(0, activeLyricIndex(lines, 1000))
        assertEquals(2, activeLyricIndex(lines, 2000))
        assertEquals(3, activeLyricIndex(lines, 9000))
        assertEquals(0, activeLyricIndex(lines, 1500))
    }
}
