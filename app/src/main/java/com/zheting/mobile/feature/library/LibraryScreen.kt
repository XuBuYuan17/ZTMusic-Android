package com.zheting.mobile.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.ui.components.PageTitle
import com.zheting.mobile.ui.components.SongRow
import com.zheting.mobile.ui.components.artistLabel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    currentSong: Song? = null,
    queueCount: Int = 0,
    onAccount: () -> Unit = {},
    onQueue: () -> Unit = {},
    onPlayer: () -> Unit = {},
    onExplore: () -> Unit = {},
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { PageTitle("资料库", subtitle = "把喜欢的声音，留在身边") }
        item {
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                onClick = onAccount,
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("我的账户", style = MaterialTheme.typography.titleLarge)
                        Text("查看登录状态", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            LibraryAction(Icons.AutoMirrored.Filled.QueueMusic, "播放队列", if (queueCount > 0) "$queueCount 首" else "暂无歌曲", onQueue)
            HorizontalDivider(Modifier.padding(start = 68.dp, end = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
            LibraryAction(Icons.Default.Info, "关于哲听", null) { showAbout = true }
        }
        item {
            Text("正在听", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
            if (currentSong != null) {
                SongRow(currentSong.name, currentSong.artistLabel, currentSong.coverUrl, onClick = onPlayer, isCurrent = true)
            } else {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryMusic, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                    Spacer(Modifier.height(16.dp))
                    Text("从一首喜欢的歌开始", style = MaterialTheme.typography.titleMedium)
                    Text("发现新歌，或搜索熟悉的旋律", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onExplore) { Text("去发现音乐") }
                }
            }
        }
        item {
            Surface(
                Modifier.padding(20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("你的收藏", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("收藏歌单与历史记录暂未开放。现在可以通过搜索和推荐歌单听音乐。",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            icon = { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("哲听") },
            text = { Text("简单一点，安静听歌。\n\n原生 Android 音乐客户端\n外观随系统切换深色与浅色。\n\n仅供个人学习与技术交流。音乐版权归各版权方所有。") },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("好") } },
        )
    }
}

@Composable
private fun LibraryAction(icon: ImageVector, title: String, detail: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, Modifier.weight(1f).padding(start = 20.dp), style = MaterialTheme.typography.titleMedium)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
