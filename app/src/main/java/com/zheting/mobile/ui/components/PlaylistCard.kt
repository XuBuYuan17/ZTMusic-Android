package com.zheting.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.zheting.mobile.ui.theme.Spacing

/**
 * 歌单卡片：方形封面 + 两行文本。封面主导、不卡片化。
 * 用于首页横向轮播 / 网格；标题长文本省略，缺失封面走 [Artwork]。
 */
@Composable
fun PlaylistCard(
    name: String,
    subtitle: String,
    coverUrl: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalArrangement = Arrangement.spacedBy(Spacing.xxSmall),
    ) {
        Artwork(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            imageUrl = coverUrl,
            cornerRadiusDp = 10,
        )
        Text(
            text = name,
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
}