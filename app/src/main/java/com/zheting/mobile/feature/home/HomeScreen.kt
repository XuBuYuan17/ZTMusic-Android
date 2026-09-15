package com.zheting.mobile.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.ui.components.EmptyView
import com.zheting.mobile.ui.components.ErrorView
import com.zheting.mobile.ui.components.LoadingView
import com.zheting.mobile.ui.components.PageTitle
import com.zheting.mobile.ui.components.PlaylistCard
import com.zheting.mobile.ui.components.SongRow
import com.zheting.mobile.ui.components.artistLabel
import com.zheting.mobile.ui.theme.Spacing

/**
 * 首页：推荐歌单（横向）+ 新歌速递（纵向）。
 * 两区独立成败，局部失败原地 ErrorView 重试，不把失败当空列表；
 * 新歌行点击整队播放（onSongClick(songs, index)），歌单卡片进详情。
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPlaylistClick: (playlistId: String) -> Unit = {},
    onSongClick: (songs: List<Song>, index: Int) -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory()),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.start() }

    when (val state = uiState) {
        HomeUiState.Loading -> LoadingView(modifier = modifier.fillMaxSize())
        is HomeUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = Spacing.xxLarge),
        ) {
            item { PageTitle(text = "哲听") }
            item { SectionHeader(text = "推荐歌单") }
            when {
                state.playlistError != null ->
                    item { ErrorView(state.playlistError, onRetry = viewModel::load) }
                state.playlists.isEmpty() ->
                    item { EmptyView("暂无推荐歌单", "换个时间再来看看") }
            }
            if (state.playlists.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.large),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        items(state.playlists, key = { it.id }) { playlist ->
                            PlaylistCard(
                                name = playlist.name,
                                subtitle = "${playlist.trackCount} 首",
                                coverUrl = playlist.coverUrl,
                                onClick = { onPlaylistClick(playlist.id) },
                                modifier = Modifier.width(152.dp),
                            )
                        }
                    }
                }
            }
            item { SectionHeader(text = "新歌速递") }
            when {
                state.newSongsError != null ->
                    item { ErrorView(state.newSongsError, onRetry = viewModel::load) }
                state.newSongs.isEmpty() ->
                    item { EmptyView("暂无新歌", "换个时间再来看看") }
            }
            itemsIndexed(state.newSongs, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    title = song.name,
                    subtitle = song.artistLabel,
                    coverUrl = song.coverUrl,
                    onClick = { onSongClick(state.newSongs, index) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.small),
    )
}