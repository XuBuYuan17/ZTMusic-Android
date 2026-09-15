package com.zheting.mobile.feature.playlist

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.ui.components.Artwork
import com.zheting.mobile.ui.components.ErrorView
import com.zheting.mobile.ui.components.LoadingView
import com.zheting.mobile.ui.components.SongRow
import com.zheting.mobile.ui.components.artistLabel
import com.zheting.mobile.ui.theme.Spacing

/**
 * 歌单详情：返回栏 + 封面头部（含已加载/总数） + 有序歌曲列表。
 * 分批补全由 PlaylistDetailViewModel 负责，滚动到底自动加载更多；
 * 歌曲行点击以「已加载曲目」为队列整队播放（onSongClick(songs, index)）。
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSongClick: (songs: List<Song>, index: Int) -> Unit = { _, _ -> },
    currentSongId: String? = null,
    viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist_$playlistId",
        factory = PlaylistDetailViewModel.factory(playlistId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(playlistId) { viewModel.start() }

    Column(modifier = modifier.fillMaxSize()) {
        // 返回栏（固定，不随列表滚动）
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        }

        when (val state = uiState) {
            PlaylistDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingView(message = "加载歌单中…")
            }
            is PlaylistDetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorView(state.message, onRetry = viewModel::load)
            }
            is PlaylistDetailUiState.Content -> PlaylistContent(state, viewModel::loadMore, onSongClick, currentSongId)
        }
    }
}

@Composable
private fun PlaylistContent(
    state: PlaylistDetailUiState.Content,
    onLoadMore: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    currentSongId: String?,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(state.songs.size, state.hasMore, state.loadingMore, state.loadMoreError) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.hasMore && !state.loadingMore && state.loadMoreError == null &&
                lastVisible >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = Spacing.huge),
    ) {
        item(key = "header") { PlaylistHeader(state) { onSongClick(state.songs, 0) } }
        if (state.songs.isEmpty()) {
            when {
                state.loadMoreError != null -> item(key = "empty_error") {
                    ErrorView(state.loadMoreError, onRetry = onLoadMore)
                }
                else -> item(key = "empty") {
                    Text(
                        text = "歌单暂无可播放歌曲",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.large),
                    )
                }
            }
        } else {
            itemsIndexed(state.songs, key = { index, song -> "$index:${song.id}" }) { index, song ->
                SongRow(
                    title = song.name,
                    subtitle = song.artistLabel,
                    coverUrl = song.coverUrl,
                    trailingText = formatDuration(song.durationMs),
                    onClick = { onSongClick(state.songs, index) },
                    isCurrent = song.id == currentSongId,
                )
            }
        }
        item(key = "footer") {
            when {
                state.loadingMore -> Box(
                    Modifier.fillMaxWidth().padding(Spacing.medium),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                state.loadMoreError != null -> Column(
                    Modifier.fillMaxWidth().padding(Spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.loadMoreError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = onLoadMore) { Text("重试") }
                }
                !state.hasMore && state.songs.isNotEmpty() -> Text(
                    text = "已加载全部 ${state.loadedCount} 首",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                )
            }
        }
    }
}


@Composable
private fun PlaylistHeader(state: PlaylistDetailUiState.Content, onPlay: () -> Unit) {
    val detail = state.detail
    var descriptionExpanded by rememberSaveable(detail.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(
            Modifier.widthIn(max = 280.dp).fillMaxWidth(0.82f).aspectRatio(1f),
            imageUrl = detail.coverUrl,
            cornerRadiusDp = 14,
        )
        Spacer(Modifier.height(24.dp))
        Text(detail.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        if (detail.creatorName.isNotBlank()) {
            Text(detail.creatorName, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center)
        }
        Text("${state.total} 首歌曲", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        if (detail.description.isNotBlank()) {
            Text(detail.description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { descriptionExpanded = !descriptionExpanded }, modifier = Modifier.align(Alignment.End)) {
                Text(if (descriptionExpanded) "收起简介" else "展开简介")
            }
        }
        Button(
            onClick = onPlay, enabled = state.songs.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.PlayArrow, null, Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (state.hasMore) "播放已加载的 ${state.songs.size} 首" else "播放全部",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

/** 时长 mm:ss；未知(<=0)返回空串。 */
private fun formatDuration(ms: Long): String {
    if (ms <= 0) return ""
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
