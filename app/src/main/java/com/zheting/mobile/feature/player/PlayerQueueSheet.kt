package com.zheting.mobile.feature.player

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zheting.mobile.playback.PlaybackUiState
import com.zheting.mobile.ui.components.EmptyView
import com.zheting.mobile.ui.components.PageTitle
import com.zheting.mobile.ui.components.SongRow
import com.zheting.mobile.ui.components.artistLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQueueSheet(state: PlaybackUiState, onPlayAt: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        PageTitle("待播清单", subtitle = "${state.queue.size} 首歌曲")
        if (state.queue.isEmpty()) {
            EmptyView("还没有待播歌曲", "从搜索或歌单中选择一首开始播放",
                Modifier.padding(vertical = 32.dp))
        } else {
            LazyColumn(
                state = rememberLazyListState(initialFirstVisibleItemIndex = state.currentIndex.coerceAtLeast(0)),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(state.queue, key = { index, entry -> "$index:${entry.song.id}" }) { index, entry ->
                    SongRow(entry.song.name, entry.song.artistLabel, entry.song.coverUrl,
                        trailingText = if (index == state.currentIndex) "当前" else (index + 1).toString(),
                        isCurrent = index == state.currentIndex, onClick = { onPlayAt(index) })
                }
            }
        }
    }
}
