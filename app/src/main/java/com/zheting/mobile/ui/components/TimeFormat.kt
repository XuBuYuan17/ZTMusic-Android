package com.zheting.mobile.ui.components

/**
 * 播放器时间 mm:ss；未知时长（<=0）返回 "--:--"。
 * 纯函数便于单测，与列表行内时长展示独立（行内未知显示空串）。
 */
fun formatPlaybackTime(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}