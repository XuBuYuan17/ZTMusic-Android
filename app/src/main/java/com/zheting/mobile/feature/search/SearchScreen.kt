package com.zheting.mobile.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.ui.components.EmptyView
import com.zheting.mobile.ui.components.ErrorView
import com.zheting.mobile.ui.components.LoadingView
import com.zheting.mobile.ui.components.PageTitle
import com.zheting.mobile.ui.components.SongRow
import com.zheting.mobile.ui.components.artistLabel
import com.zheting.mobile.ui.theme.Spacing

/**
 * 搜索页。搜索框 + 结果流。
 * - 旧响应覆盖新关键词由 SearchViewModel 负责（关键词隔离 + 协程取消）；
 * - 列表滚动接近底部自动加载更多，失败保留结果并原地重试；
 * - 结果行点击整队播放（onSongClick(songs, index)）。
 */
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onSongClick: (songs: List<Song>, index: Int) -> Unit = {},
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory()),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuery = (uiState as? SearchUiState.Content)?.query ?: ""
    var queryText by rememberSaveable { mutableStateOf(currentQuery) }

    Column(modifier = modifier.fillMaxSize()) {
        PageTitle(text = "搜索")
        SearchField(
            query = queryText,
            onQueryChange = { queryText = it },
            onSearch = viewModel::search,
        )
        when (val state = uiState) {
            SearchUiState.Idle -> CenteredBox { EmptyView("搜索歌曲", "输入关键词，查找网易云音乐歌曲") }
            SearchUiState.Loading -> CenteredBox { LoadingView(message = "正在搜索…") }
            is SearchUiState.Content -> SearchResults(
                state = state,
                onRetrySearch = viewModel::search,
                onLoadMore = viewModel::loadMore,
                onSongClick = onSongClick,
            )
        }
    }
}

/** 结果列表：首屏失败整页重试；有结果时底部自动加载更多，失败保留结果原地重试。 */
@Composable
private fun SearchResults(
    state: SearchUiState.Content,
    onRetrySearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
) {
    if (state.songs.isEmpty()) {
        if (state.error != null) {
            CenteredBox {
                ErrorView(state.error, onRetry = { onRetrySearch(state.query) })
            }
        } else {
            CenteredBox { EmptyView("没有找到相关歌曲", "换个关键词试试") }
        }
        return
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember(state.songs.size, state.hasMore, state.error) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.hasMore && state.error == null && lastVisible >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = Spacing.huge),
    ) {
        itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                title = song.name,
                subtitle = song.artistLabel,
                coverUrl = song.coverUrl,
                trailingText = formatDuration(song.durationMs),
                onClick = { onSongClick(state.songs, index) },
            )
        }
        // 页脚：加载更多进行中 / 失败重试 / 全部加载完
        when {
            state.error != null -> item {
                FooterSection {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = onLoadMore, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("重试")
                    }
                }
            }
            state.hasMore -> item {
                Box(Modifier.fillMaxWidth().padding(Spacing.medium), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> item {
                Text(
                    text = "已显示全部 ${state.songs.size} 首",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                )
            }
        }
    }
}

/** 页脚容器：横向居中布局，用于加载状态与重试块。 */
@Composable
private fun FooterSection(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** 时长 mm:ss；未知(<=0)返回空串。 */
private fun formatDuration(ms: Long): String {
    if (ms <= 0) return ""
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large),
        placeholder = { Text("搜索歌曲") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
    )
}