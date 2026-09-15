package com.zheting.mobile.playback

import com.zheting.mobile.core.model.Song

/** 队列里的一个条目：歌曲 + 是否试听（当前解析结果的标记）。 */
data class QueueEntry(
    val song: Song,
    val isTrial: Boolean = false,
)

/** 对外暴露的播放状态（UI / 媒体通知同一份数据源）。 */
data class PlaybackUiState(
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = INDEX_UNSET,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
) {
    val currentSong: Song? get() = queue.getOrNull(currentIndex)?.song

    /** 未就绪（无播放目标）且无错误 → 队列尚未设定。 */
    val hasTarget: Boolean get() = currentIndex != INDEX_UNSET

    companion object {
        const val INDEX_UNSET = -1
    }
}