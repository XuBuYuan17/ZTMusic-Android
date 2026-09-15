package com.zheting.mobile.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zheting.mobile.ui.components.EmptyView

@Composable
fun LyricsPanel(
    lines: List<LyricLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "当前版本暂不支持歌词显示",
) {
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyView("让音乐继续", emptyMessage)
        }
        return
    }
    val active = remember(lines, positionMs) { activeLyricIndex(lines, positionMs) }
    val listState = rememberLazyListState()
    val dragging by listState.interactionSource.collectIsDraggedAsState()
    var followPlayback by remember(lines) { mutableStateOf(true) }
    LaunchedEffect(dragging) { if (dragging) followPlayback = false }
    LaunchedEffect(active, followPlayback) {
        if (followPlayback && active >= 0) listState.animateScrollToItem(active)
    }
    Column(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(lines, key = { index, line -> "$index:${line.timeMs}" }) { index, line ->
                Text(
                    line.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (index == active) 1f else 0.38f),
                    modifier = Modifier.fillMaxWidth().semantics { selected = index == active }
                        .clickable(onClickLabel = "从这句播放") { onSeek(line.timeMs) }
                        .padding(vertical = 8.dp),
                )
            }
        }
        if (!followPlayback) {
            TextButton(onClick = { followPlayback = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("回到当前歌词")
            }
        }
    }
}
