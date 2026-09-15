package com.zheting.mobile.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zheting.mobile.playback.PlaybackUiState
import com.zheting.mobile.ui.components.Artwork
import com.zheting.mobile.ui.components.artistLabel
import com.zheting.mobile.ui.components.formatPlaybackTime
import com.zheting.mobile.ui.theme.Spacing

/**
 * 全屏播放器（Apple Music 参考的静态布局：顶栏 → 大封面 → 歌曲信息 → 控制 → 进度）。
 * - 进度条拖动时由本地预览值驱动，松手才提交 seek；未知时长禁用拖动；
 * - 系统返回 / 关闭按钮共用同一 onClose 流程；播放状态读写全部来自 PlaybackController，
 *   页面切换不重建、不重载。
 */
@Composable
fun FullPlayerScreen(
    state: PlaybackUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueue: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    BackHandler(onBack = onClose)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Spacing.xLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶栏：收起 / 标题 / 队列
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "收起播放器")
                }
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onQueue) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "播放队列")
                }
            }

            // 封面：占满中部可用空间（上限 420dp，小屏不溢出）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Artwork(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(1f)
                        .heightIn(max = 420.dp),
                    imageUrl = song.coverUrl,
                    cornerRadiusDp = 8,
                )
            }

            // 歌曲信息
            Text(
                text = song.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(Spacing.xxSmall))
            Text(
                text = song.artistLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.xSmall),
                )
            }

            // 控制区：上一首 / 播放 / 下一首（未实现的随机、循环不做死按钮占位）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xLarge),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(Spacing.xLarge))
                IconButton(onClick = onTogglePlay, enabled = !state.isLoading, modifier = Modifier.size(72.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = MaterialTheme.colorScheme.background,
                                strokeWidth = 2.5.dp,
                                trackColor = Color.Transparent,
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "暂停" else "播放",
                                modifier = Modifier.size(38.dp),
                                tint = MaterialTheme.colorScheme.background,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(Spacing.xLarge))
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一首", modifier = Modifier.size(32.dp))
                }
            }

            PlayerProgress(state, onSeek)

            Spacer(Modifier.size(Spacing.small))
        }
    }
}

/** 进度条 + 时间标签；拖动预览本地位置，松手提交 seek。 */
@Composable
private fun PlayerProgress(
    state: PlaybackUiState,
    onSeek: (Long) -> Unit,
) {
    val durationMs = state.durationMs.coerceAtLeast(0L)
    val isSeekable = durationMs > 0L
    val maxMs = if (isSeekable) durationMs else 1L

    var dragMs by remember { mutableStateOf<Long?>(null) }
    // 切歌/时长变化时丢弃遗留拖动预览
    LaunchedEffect(state.currentSong?.id, durationMs) { dragMs = null }

    val displayMs = (dragMs ?: state.positionMs.coerceIn(0L, durationMs)).coerceIn(0L, maxMs)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlaybackTime(displayMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(44.dp),
            )
            Slider(
                value = displayMs.toFloat(),
                onValueChange = { dragMs = it.toLong() },
                valueRange = 0f..maxMs.toFloat(),
                onValueChangeFinished = {
                    dragMs?.let(onSeek)
                    dragMs = null
                },
                enabled = isSeekable,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.onSurface,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    thumbColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPlaybackTime(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(44.dp),
            )
        }
    }
}

/** 播放队列底部弹层：序号 + 歌曲，高亮当前，点击跳播。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueSheet(
    state: PlaybackUiState,
    onPlayAt: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (state.queue.isEmpty()) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "播放队列",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.small),
            )
            LazyColumn {
                itemsIndexed(state.queue, key = { _, entry -> entry.song.id }) { index, entry ->
                    val isCurrent = index == state.currentIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayAt(index) }
                            .padding(horizontal = Spacing.large, vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = (index + 1).toString().padStart(2, '0'),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.song.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = entry.song.artistLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "正在播放",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}