package com.zheting.mobile.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * 动画令牌（供后续播放器过渡/歌词滚动统一使用）。
 * 原则：克制的时长、标准缓动，可被系统动画设置覆盖（loop 10 接入）。
 */
object Motion {
    /** 瞬时交互反馈（按压回弹、迷你切换）。 */
    const val FastMs = 150

    /** 常规内容过渡。 */
    const val StandardMs = 300

    /** 大范围连续过渡（Mini → 全屏）。 */
    const val SlowMs = 480

    val FastOutSlowIn = tween<Float>(StandardMs, easing = FastOutSlowInEasing)

    val Standard = tween<Float>(StandardMs, easing = FastOutSlowInEasing)

    val Slow = tween<Float>(SlowMs, easing = FastOutSlowInEasing)

    /** 收起/退出的反向缓动。 */
    val Exit = tween<Float>(StandardMs, easing = LinearOutSlowInEasing)
}