package com.zheting.mobile.playback

import android.app.PendingIntent
import androidx.annotation.OptIn
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zheting.mobile.MainActivity
import com.zheting.mobile.ZTMusicApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * 唯一的播放器宿主（契约：Service 内唯一 ExoPlayer 与 MediaSession）。
 * - 音频焦点与耳机拔出：setAudioAttributes（焦点）＋ setHandleAudioBecomingNoisy（拔出暂停）；
 * - 后台播放：前台服务 + 默认媒体通知（DefaultMediaNotificationProvider 自动建 channel）；
 * - onTaskRemoved 不主动停服：滑动清除任务后继续播放。
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private lateinit var controller: PlaybackController
    private lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val app = application as ZTMusicApp

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent())
            .build()
        // 默认媒体通知：自动创建 "playback" channel，图标/动作用系统默认
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        controller = PlaybackController(player, PlaybackUrlResolverImpl(app.container.api), scope)
        PlaybackBridge.attach(controller)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    /** 滑动清除任务后不主动停止：后台播放继续（前台服务 + stopWithTask=false）。 */
    override fun onTaskRemoved(rootIntent: Intent?) = Unit

    override fun onDestroy() {
        PlaybackBridge.detach(controller)
        controller.release()
        session.release()
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun sessionActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/**
 * 同进程桥：Service 持有事实上的控制器，页面经此取实例（进程内单例，无跨进程开销）。
 * StateFlow 便于 Compose 响应控制器到来/离开；awaitController 兜首次启动服务的竞态。
 */
object PlaybackBridge {
    private val _controller = MutableStateFlow<PlaybackController?>(null)
    val controllerFlow: StateFlow<PlaybackController?> = _controller.asStateFlow()

    val controller: PlaybackController? get() = _controller.value

    fun attach(c: PlaybackController) {
        _controller.value = c
    }

    fun detach(c: PlaybackController) {
        if (_controller.value === c) _controller.value = null
    }

    /** 挂起直到控制器可用（Service onCreate 是异步的）。控制器常驻则立即返回。 */
    suspend fun awaitController(): PlaybackController = _controller.filterNotNull().first()
}