package com.zheting.mobile.ui

import com.zheting.mobile.ui.components.formatPlaybackTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** 播放器时间显示 mm:ss；未知时长（<=0）显示 --:--（播放器进度条与全屏时间标签共用）。 */
class TimeFormatTest {

    @Test
    fun zero_unknownShowsDash() {
        assertEquals("--:--", formatPlaybackTime(0))
    }

    @Test
    fun negative_unknownShowsDash() {
        assertEquals("--:--", formatPlaybackTime(-1))
    }

    @Test
    fun secondsRollToMinutes() {
        assertEquals("1:05", formatPlaybackTime(65_000L))
    }

    @Test
    fun fullMinute() {
        assertEquals("3:00", formatPlaybackTime(180_000L))
    }

    @Test
    fun hoursCarryIntoMinutes() {
        assertEquals("60:00", formatPlaybackTime(3_600_000L))
    }
}