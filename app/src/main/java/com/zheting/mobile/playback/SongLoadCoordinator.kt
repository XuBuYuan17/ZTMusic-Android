package com.zheting.mobile.playback

import com.zheting.mobile.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 一次装载旅程的结果（供控制器决定后续动作：开播 / 报无音源）。 */
sealed interface SongLoadResult {
    data class Ready(val candidate: AudioCandidate, val song: Song) : SongLoadResult
    data class Exhausted(val song: Song) : SongLoadResult
}

/**
 * 歌曲装载协调器：唯一的「当前歌 → 候选序列 → 按序装载」入口。
 *
 * 隔离与取消（对齐原项目 playTrack 的 requestId 守卫）：
 * - 快速切歌：playSong 先取消在飞 Job（含正在等待 sink.load 的挂起点）；
 * - 迟到响应：每个目标取自增 token，异步阶段落地前校验 token 未变，变了即丢弃；
 * - 装载失败换源：Failed → 尝试下一候选，位置沿用失败时进度（同曲换源进度恢复）；
 * - 空 URL 候选直接跳过，不触发装载；
 * - 有限换源：单曲尝试数封顶 [MAX_CANDIDATE_ATTEMPTS]，防病态候选无限刷。
 * - 结果：每次旅程终结点回调 [onResult]（Ready 携带当选候选，含试听标记）。
 */
class SongLoadCoordinator(
    private val resolver: PlaybackUrlResolver,
    private val sink: MediaSink,
    private val scope: CoroutineScope,
    private val onResult: (SongLoadResult) -> Unit = {},
) {
    private var playToken = 0L
    private var job: Job? = null

    /** 当前目标歌曲 id（请求属主校对），null 表示空闲。 */
    private var targetSongId: String? = null
    val currentTarget: String? get() = targetSongId

    /**
     * 装载/切换歌曲。
     * @param startPositionMs 0 从头；>0（恢复中断、seek 后重载）沿用该位置。
     */
    fun playSong(song: Song, startPositionMs: Long = 0L) {
        playToken++
        targetSongId = song.id
        job?.cancel()
        job = scope.launch {
            val token = playToken
            val candidates = resolver.resolve(song)
            // 期间已切歌：迟到的候选列表整体丢弃
            if (token != playToken) return@launch

            var position = startPositionMs
            var tried = 0
            for (candidate in candidates) {
                if (token != playToken) return@launch // 每一部落地前校验
                if (!candidate.usable) continue
                if (++tried > MAX_CANDIDATE_ATTEMPTS) break
                when (val outcome = sink.load(candidate.url, song, candidate.isTrial, position)) {
                    LoadOutcome.Success -> {
                        if (token == playToken) onResult(SongLoadResult.Ready(candidate, song))
                        return@launch
                    }
                    is LoadOutcome.Failed -> {
                        if (token != playToken) return@launch
                        position = outcome.positionMs // 同曲换源：沿用失败前进度
                    }
                }
            }
            if (token == playToken) {
                sink.onExhausted(song)
                onResult(SongLoadResult.Exhausted(song))
            }
        }
    }

    /** 停止当前装载并清空目标（如整队清空、服务销毁前）。 */
    fun stop() {
        playToken++
        targetSongId = null
        job?.cancel()
        job = null
    }

    companion object {
        /** 有限换源：单曲尝试候选数上限（原项目候选中 long → max 已在 parse 层收敛）。 */
        const val MAX_CANDIDATE_ATTEMPTS = 8
    }
}