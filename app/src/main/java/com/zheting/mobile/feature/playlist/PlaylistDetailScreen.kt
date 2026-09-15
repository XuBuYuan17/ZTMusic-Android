package com.zheting.mobile.feature.playlist

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
    onSongClick: (songs: List<Song>, index: Int) -> Unit = {},
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
            is PlaylistDetailUiState.Content -> PlaylistContent(state, viewModel::loadMore, onSongClick)
        }
    }
}

@Composable
private fun PlaylistContent(
    state: PlaylistDetailUiState.Content,
    onLoadMore: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
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
        item(key = "header") { PlaylistHeader(state) }
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
            itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    title = song.name,
                    subtitle = song.artistLabel,
                    coverUrl = song.coverUrl,
                    trailingText = formatDuration(song.durationMs),
                    onClick = { onSongClick(state.songs, index) },
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

/** 封面头部：方形封面 + 歌名/创建者/加载进度，文字自适应剩余宽度。 */
@Composable
private fun PlaylistHeader(state: PlaylistDetailUiState.Content) {
    val detail = state.detail
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            modifier = Modifier.size(160.dp),
            imageUrl = detail.coverUrl,
            cornerRadiusDp = 12,
        )
        Spacer(Modifier.width(Spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xSmall))
            Text(
                text = detail.creatorName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "已加载 ${state.loadedCount.coerceAtMost(state.total)} / ${state.total} 首",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (detail.description.isNotBlank()) {
                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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