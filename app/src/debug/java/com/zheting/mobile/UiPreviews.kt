package com.zheting.mobile

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zheting.mobile.core.model.Artist
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.feature.library.LibraryScreen
import com.zheting.mobile.feature.player.FullPlayerScreen
import com.zheting.mobile.feature.player.LyricLine
import com.zheting.mobile.feature.player.LyricsPanel
import com.zheting.mobile.feature.player.MiniPlayer
import com.zheting.mobile.playback.PlaybackUiState
import com.zheting.mobile.playback.QueueEntry
import com.zheting.mobile.ui.components.SongRow

// 预览数据只存在于 debug source set，不接入运行时页面。
private val previewSong = Song(
    id = "preview",
    name = "把夜晚留给音乐",
    artists = listOf(Artist("preview", "哲听 · 界面预览")),
    durationMs = 246000,
)
private val previewPlayer = PlaybackUiState(
    queue = listOf(QueueEntry(previewSong)),
    currentIndex = 0, isPlaying = true, positionMs = 92000, durationMs = 246000,
)

@Preview(name = "播放器 · 浅色", widthDp = 360, heightDp = 740)
@Preview(name = "播放器 · 深色", widthDp = 360, heightDp = 740, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "播放器 · 小屏大字体", widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Preview(name = "播放器 · 横屏", widthDp = 740, heightDp = 360)
@Composable
private fun PlayerPreview() {
    ZTMusicTheme {
        FullPlayerScreen(previewPlayer, onTogglePlay = {}, onNext = {}, onPrevious = {}, onSeek = {}, onQueue = {}, onClose = {})
    }
}

@Preview(name = "资料库 · 游客", widthDp = 360, heightDp = 740)
@Preview(name = "资料库 · 深色大字体", widthDp = 320, heightDp = 740, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LibraryPreview() {
    ZTMusicTheme { Surface { LibraryScreen() } }
}

@Preview(name = "Mini 与歌曲行", widthDp = 360, heightDp = 230)
@Composable
private fun ComponentsPreview() {
    ZTMusicTheme {
        Surface {
            Column(Modifier.padding(12.dp)) {
                MiniPlayer(previewPlayer, onExpand = {}, onTogglePlay = {}, onNext = {})
                SongRow(previewSong.name, "哲听 · 界面预览", isCurrent = true)
                SongRow("很长的歌曲名称用来检查单行省略是否正确", "很长的歌手名称 / 第二位歌手")
            }
        }
    }
}

@Preview(name = "歌词 · 排版与当前行", widthDp = 360, heightDp = 420)
@Composable
private fun LyricsPreview() {
    ZTMusicTheme {
        Surface {
            LyricsPanel(
                lines = listOf(LyricLine(0, "这是歌词界面预览"), LyricLine(1000, "当前句清晰呈现"),
                    LyricLine(2000, "留白，让文字自然呼吸"), LyricLine(3000, "点击一句，可以跳到对应时间")),
                positionMs = 1000,
                onSeek = {},
                modifier = Modifier.fillMaxSize().padding(28.dp),
            )
        }
    }
}
