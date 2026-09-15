package com.zheting.mobile.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.zheting.mobile.core.model.Song
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * MediaSink 的 ExoPlayer 落地实现。
 * - load：清空旧装载态 → 装配带元数据的 MediaItem → prepare → 挂起至 READY 或 error；
 * - 挂起期间被取消（快速切歌）会卸载 listener，不干扰后续装载；
 * - 无 URL/耗尽时的清场由 [onExhausted] 负责，错误文案由协调器结果回调上抛。
 */
@UnstableApi
class ExoMediaSink(
    private val player: ExoPlayer,
) : MediaSink {

    /** 表示当前正由一个 load 挂起等待，屏蔽其他错误事件的误报。 */
    private var awaitingLoad = false

    override suspend fun load(url: String, song: Song, isTrial: Boolean, startPositionMs: Long): LoadOutcome {
        player.stop() // 清掉上一个目标的装载态（不释放）
        player.setMediaItem(buildItem(url, song), startPositionMs)
        player.prepare()
        return awaitReadyOrError()
    }

    override suspend fun onExhausted(song: Song) {
        player.stop()
        player.clearMediaItems()
    }

    private fun buildItem(url: String, song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.name)
            .setArtist(song.artists.joinToString(" / ") { it.name })
            .setArtworkUri(song.coverUrl.takeIf(String::isNotBlank)?.let(Uri::parse))
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }

    /** 挂起至 player 进入 READY（成功）或抛错（失败，带回失败前位置）。 */
    private suspend fun awaitReadyOrError(): LoadOutcome = suspendCancellableCoroutine { cont ->
        awaitingLoad = true
        val listener = object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (!awaitingLoad) return
                if (playbackState == Player.STATE_READY) {
                    awaitingLoad = false
                    player.removeListener(this)
                    cont.resume(LoadOutcome.Success)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!awaitingLoad) return
                awaitingLoad = false
                player.removeListener(this)
                cont.resume(LoadOutcome.Failed(player.currentPosition.coerceAtLeast(0)))
            }
        }
        player.addListener(listener)
        cont.invokeOnCancellation {
            awaitingLoad = false
            player.removeListener(listener)
        }
    }
}