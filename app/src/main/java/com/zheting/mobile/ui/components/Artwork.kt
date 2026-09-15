package com.zheting.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * 封面组件。优先加载真实 URL（Coil 独立实例，不挂会话拦截器，契约 §1.6）；
 * URL 缺失或加载失败时回落到品牌渐变 + 音符占位，保证破图可读。
 */
@Composable
fun Artwork(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    cornerRadiusDp: Int = 8,
) {
    val primary = MaterialTheme.colorScheme.primary
    val fallback = MaterialTheme.colorScheme.secondaryContainer
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadiusDp.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // 占位基底（URL 缺失/加载中/失败时可见）
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.85f), fallback))),
        )
        Text(
            text = "♪",
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.9f),
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}