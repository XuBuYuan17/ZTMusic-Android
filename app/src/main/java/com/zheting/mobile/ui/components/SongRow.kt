package com.zheting.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.ui.theme.Spacing

/** 歌曲署名：优先歌手，其次专辑，兜底未知。 */
val Song.artistLabel: String
    get() = artists.joinToString(" / ") { it.name }
        .ifEmpty { album?.name?.takeIf { it.isNotEmpty() } ?: "" }
        .ifEmpty { "未知歌手" }

/**
 * 歌曲行（Apple Music 风格：非卡片、封面主导、两行文本）。
 * - 最小高度 48dp（无障碍触区），大字体下可自然增高；
 * - 长文本单行省略；封面用 [Artwork] 占位（缺失封面可读）；
 * - trailing 预留时长文本位（数据接入后展示）。
 */
@Composable
fun SongRow(
    title: String,
    subtitle: String,
    coverUrl: String? = null,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Spacing.huge)
            .padding(vertical = Spacing.xSmall)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = Spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            modifier = Modifier.size(40.dp),
            imageUrl = coverUrl,
            cornerRadiusDp = 6,
        )
        Spacer(Modifier.width(Spacing.small))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.small),
            )
        }
    }
}