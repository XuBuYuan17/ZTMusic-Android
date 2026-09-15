package com.zheting.mobile.playback

import com.zheting.mobile.core.model.Song

/**
 * 单个候选音频地址。url 为空串视为「无可用地址」（候选层跳过，不入装载）。
 * isTrial 标记试听候选（原项目试听提示逻辑）。
 */
data class AudioCandidate(
    val url: String,
    val isTrial: Boolean = false,
) {
    val usable: Boolean get() = url.isNotBlank()
}

/**
 * 歌曲 → 可播候选地址序列。按原项目筛选顺序返回：
 * 普通接口音质尝试 → unblock → match → 旧接口 → 已收集的试听候选 → 官方外链。
 * 具体端点与降级见 [PlaybackUrlResolverImpl]。
 */
interface PlaybackUrlResolver {
    suspend fun resolve(song: Song): List<AudioCandidate>
}

/** 一次装载的结果：成功 / 直接失败（此 URL 地址不可用，需换源）。 */
sealed interface LoadOutcome {
    /** 装载成功：当前 URL 可用，开播。 */
    data object Success : LoadOutcome
    /**
     * 该 URL 实际加载失败（网络/解码/404），应由调度器尝试下一候选。
     * positionMs：失败前已推进到的位置（同曲换源进度恢复用，从头失败则为 0）。
     */
    data class Failed(val positionMs: Long) : LoadOutcome
}

/**
 * 播放器的装载抽象：把协调器与具体 ExoPlayer 解耦（协调器可脱离 Android 单测）。
 * 同一时刻只应有一个调用者在挂起等待结果。
 */
interface MediaSink {
    /**
     * 以 url 装载歌曲。挂起直到 ExoPlayer 给出结果：
     * - 进入可播放状态/切换完成 → Success；
     * - Player error → Failed（换源候选）。
     * startPositionMs > 0 时（同曲换源进度恢复）从该位置开始。
     */
    suspend fun load(url: String, song: Song, isTrial: Boolean, startPositionMs: Long): LoadOutcome

    /** 全部候选耗尽：清停播放、上报「暂无可用音源」。 */
    suspend fun onExhausted(song: Song)
}