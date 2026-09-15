package com.zheting.mobile.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.zheting.mobile.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 播放控制中心：UI/媒体通知共用同一 [PlaybackUiState]。
 * - 顺序队列（list 循环语义对齐原项目 queue.ts：到底回卷）；
 * - 歌曲装载交给 [SongLoadCoordinator]（快速切歌取消 + 迟到响应隔离在这里收敛）；
 * - 进度由常驻 ticker 驱动，暂停时也持续同步（seek 后立即反馈）。
 */
@UnstableApi
class PlaybackController(
    private val player: ExoPlayer,
    resolver: PlaybackUrlResolver,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val coordinator = SongLoadCoordinator(resolver, ExoMediaSink(player), scope, onResult = ::onLoadResult)

    private val queue = mutableListOf<QueueEntry>()
    private var currentIndex = PlaybackUiState.INDEX_UNSET
    private var isLoading = false
    private var progressJob: Job? = null

    init {
        player.addListener(playerListener)
        progressJob = scope.launch {
            while (isActive) {
                pushState()
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    // ---- 队列与播放 ----

    /** 整队播放（对齐 playQueue）。startIndex 越界自动收敛到队内。 */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        queue.clear()
        queue.addAll(songs.map { QueueEntry(it) })
        currentIndex = if (queue.isEmpty()) PlaybackUiState.INDEX_UNSET
        else startIndex.coerceIn(0, queue.lastIndex)
        loadCurrent(startPositionMs = 0)
    }

    /** 跳到队内某首并播放。 */
    fun playAt(index: Int) {
        if (queue.isEmpty() || index !in 0..queue.lastIndex) return
        currentIndex = index
        loadCurrent(startPositionMs = 0)
    }

    fun next() {
        if (queue.isEmpty()) return
        currentIndex = if (currentIndex == PlaybackUiState.INDEX_UNSET) 0
        else (currentIndex + 1).mod(queue.size) // list 循环回卷
        loadCurrent(startPositionMs = 0)
    }

    fun previous() {
        if (queue.isEmpty()) return
        if (currentIndex == PlaybackUiState.INDEX_UNSET) {
            currentIndex = 0
            loadCurrent(0)
            return
        }
        // 已播过开头：回到本曲开头；否则回上一首
        if (player.currentPosition > RESTART_THRESHOLD_MS) {
            seekTo(0)
            return
        }
        currentIndex = (currentIndex - 1 + queue.size).mod(queue.size)
        loadCurrent(startPositionMs = 0)
    }

    fun play() {
        if (player.playbackState != Player.STATE_ENDED) player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlay() {
        if (player.isPlaying) pause() else play()
    }

    /** 拖动进度条：提交 seek（未知时长/负值由 UI 禁用，这里兜底收敛）。 */
    fun seekTo(positionMs: Long) {
        val d = player.duration
        val target = if (d > 0) positionMs.coerceIn(0, d) else positionMs
        player.seekTo(target)
        pushState()
    }

    /** 服务销毁前收尾：停循环与装载，不释放 player（由 Service 负责）。 */
    fun release() {
        progressJob?.cancel()
        progressJob = null
        player.removeListener(playerListener)
        coordinator.stop()
    }

    // ---- 内部 ----

    private fun loadCurrent(startPositionMs: Long) {
        val entry = queue.getOrNull(currentIndex) ?: return
        isLoading = true
        pushState()
        coordinator.playSong(entry.song, startPositionMs)
    }

    private fun onLoadResult(result: SongLoadResult) {
        when (result) {
            is SongLoadResult.Ready -> {
                isLoading = false
                queue.setTrialAt(currentIndex, result.candidate.isTrial)
                // 出音成功后清除此前错误展示
                if (_uiState.value.error != null) _uiState.update { it.copy(error = null) }
                pushState()
                player.play()
            }
            is SongLoadResult.Exhausted -> {
                isLoading = false
                player.stop()
                _uiState.update { it.copy(error = "当前歌曲暂无可用音源") }
                pushState()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            pushState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && player.playWhenReady) {
                next() // 顺序队列到末尾自动回卷下一首
                return
            }
            pushState()
        }

        override fun onPlayerError(error: PlaybackException) {
            // 换源逻辑由 sink 在装载挂起点处理；这里只负责状态同步
            pushState()
        }
    }

    private fun pushState() {
        _uiState.value = PlaybackUiState(
            queue = queue.toList(),
            currentIndex = currentIndex,
            isPlaying = player.isPlaying,
            isLoading = isLoading,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            error = _uiState.value.error,
        )
    }

    private fun MutableList<QueueEntry>.setTrialAt(index: Int, trial: Boolean) {
        if (index in indices) set(index, get(index).copy(isTrial = trial))
    }

    companion object {
        private const val PROGRESS_TICK_MS = 500L
        private const val RESTART_THRESHOLD_MS = 3000L
    }
}