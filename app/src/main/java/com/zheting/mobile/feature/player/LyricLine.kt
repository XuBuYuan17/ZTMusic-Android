package com.zheting.mobile.feature.player

/** UI 输入按时间升序排列；网络格式转换由后续歌词 Repository 负责。 */
data class LyricLine(val timeMs: Long, val text: String)

internal fun activeLyricIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var left = 0
    var right = lines.lastIndex
    var result = -1
    while (left <= right) {
        val middle = (left + right) ushr 1
        if (lines[middle].timeMs <= positionMs) {
            result = middle
            left = middle + 1
        } else {
            right = middle - 1
        }
    }
    return result
}
