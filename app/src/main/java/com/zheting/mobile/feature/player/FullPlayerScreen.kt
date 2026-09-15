package com.zheting.mobile.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zheting.mobile.playback.PlaybackUiState
import com.zheting.mobile.ui.components.Artwork
import com.zheting.mobile.ui.components.artistLabel
import com.zheting.mobile.ui.components.formatPlaybackTime

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
    artworkModifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    val close by rememberUpdatedState(onClose)
    BackHandler(onBack = onClose)
    var lyricsVisible by rememberSaveable { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val offset by animateFloatAsState(
        dragOffset,
        animationSpec = if (dragging) snap() else spring(dampingRatio = 0.86f, stiffness = 450f),
        label = "playerDrag",
    )
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier.fillMaxSize().graphicsLayer {
            translationY = offset
            val shrink = (offset / size.height.coerceAtLeast(1f)).coerceIn(0f, 0.08f)
            scaleX = 1f - shrink
            scaleY = 1f - shrink
            shape = RoundedCornerShape((shrink * 400).dp)
            clip = true
        }.background(
            Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.surface,
            )),
        ).systemBarsPadding(),
    ) {
        val height = maxHeight
        // ponytail: 只在顶栏与封面识别收起手势，避免抢占歌词滚动和进度拖动；全屏边缘手势可后续扩展。
        val dismissGesture = Modifier.pointerInput(height, density) {
            val velocity = VelocityTracker()
            detectVerticalDragGestures(
                onDragStart = { dragging = true; velocity.resetTracking() },
                onDragCancel = { dragging = false; dragOffset = 0f },
                onDragEnd = {
                    dragging = false
                    val dismiss = shouldDismissPlayer(
                        dragOffset / density.density, height.value,
                        velocity.calculateVelocity().y / density.density,
                    )
                    if (dismiss) close()
                    dragOffset = 0f
                },
            ) { change, amount ->
                change.consume()
                dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                velocity.addPosition(change.uptimeMillis, Offset(0f, dragOffset))
            }
        }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().then(dismissGesture).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "收起播放器") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(32.dp).height(4.dp).background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
                    Text("正在播放", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
                IconButton(onClick = onQueue) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放队列") }
            }
            if (maxWidth > maxHeight) {
                Row(Modifier.weight(1f).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(0.42f).fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
                        PlayerHero(state, lyricsVisible, artworkModifier, dismissGesture)
                    }
                    Column(Modifier.weight(0.58f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                        PlayerControls(state, lyricsVisible, { lyricsVisible = !lyricsVisible }, onTogglePlay, onPrevious, onNext, onSeek, onQueue)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp)
                        .heightIn(min = (height - 56.dp).coerceAtLeast(0.dp)),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Box(
                        Modifier.fillMaxWidth().height((height - 390.dp).coerceIn(172.dp, 380.dp)).padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayerHero(state, lyricsVisible, artworkModifier, dismissGesture)
                    }
                    PlayerControls(state, lyricsVisible, { lyricsVisible = !lyricsVisible }, onTogglePlay, onPrevious, onNext, onSeek, onQueue)
                }
            }
        }
    }
}

@Composable
private fun PlayerHero(
    state: PlaybackUiState,
    lyricsVisible: Boolean,
    artworkModifier: Modifier,
    dismissGesture: Modifier,
) {
    val scale by animateFloatAsState(if (state.isPlaying || state.isLoading) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.85f), label = "coverPlaybackScale")
    if (lyricsVisible) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Artwork(artworkModifier.size(52.dp).then(dismissGesture), state.currentSong?.coverUrl, 8)
            LyricsPanel(modifier = Modifier.weight(1f), lines = emptyList(), positionMs = state.positionMs, onSeek = {})
        }
    } else {
        Box(Modifier.fillMaxSize().then(dismissGesture), contentAlignment = Alignment.Center) {
            Artwork(
                artworkModifier.aspectRatio(1f, matchHeightConstraintsFirst = true).widthIn(max = 360.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; shadowElevation = 12.dp.toPx(); shape = RoundedCornerShape(12.dp); clip = true },
                state.currentSong?.coverUrl, 12,
            )
        }
    }
}

@Composable
private fun PlayerControls(
    state: PlaybackUiState,
    lyricsVisible: Boolean,
    onLyrics: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueue: () -> Unit,
) {
    val song = state.currentSong ?: return
    Column(Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
        Text(song.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(song.artistLabel, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (state.error != null) {
            Text(state.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        PlayerProgress(state, onSeek)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipPrevious, "上一首", Modifier.size(40.dp))
            }
            IconButton(onClick = onTogglePlay, enabled = !state.isLoading, modifier = Modifier.size(80.dp)) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(36.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "暂停" else "播放", Modifier.size(60.dp))
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.SkipNext, "下一首", Modifier.size(40.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconToggleButton(checked = lyricsVisible, onCheckedChange = { onLyrics() }) {
                Icon(Icons.Default.Subtitles, if (lyricsVisible) "显示封面" else "显示歌词",
                    tint = if (lyricsVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (state.isLoading) "正在准备播放…" else if (state.isPlaying) "用心听见" else "已暂停",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = onQueue) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, "播放队列", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlayerProgress(state: PlaybackUiState, onSeek: (Long) -> Unit) {
    val duration = state.durationMs.coerceAtLeast(0L)
    var dragMs by remember(state.currentSong?.id, duration) { mutableStateOf<Long?>(null) }
    val position = (dragMs ?: state.positionMs).coerceIn(0L, duration)
    Column {
        Slider(
            value = position.toFloat(),
            onValueChange = { dragMs = it.toLong() },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            enabled = duration > 0L,
            onValueChangeFinished = { dragMs?.let(onSeek); dragMs = null },
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f),
                thumbColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (position == 0L) "0:00" else formatPlaybackTime(position),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (duration == 0L) "--:--" else "−" + if (duration == position) "0:00" else formatPlaybackTime(duration - position),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
