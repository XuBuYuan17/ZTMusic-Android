package com.zheting.mobile.playback

import android.content.Context
import android.content.Intent
import com.zheting.mobile.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 点歌入口：确保播放服务在运行，拿到控制器后整队播放。
 * - 页面不直接 new 播放器；控制器唯一实例在 [PlaybackService]，这里只负责启动与转发；
 * - Service 已运行时启动是无害的，playQueue 会整队替换，无重复装载。
 */
object PlaybackLauncher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun play(context: Context, songs: List<Song>, startIndex: Int) {
        context.startForegroundService(Intent(context, PlaybackService::class.java))
        scope.launch {
            val controller = PlaybackBridge.awaitController()
            controller.playQueue(songs, startIndex)
        }
    }
}