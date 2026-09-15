package com.zheting.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.zheting.mobile.ui.theme.AppShapes
import com.zheting.mobile.ui.theme.AppTypography
import com.zheting.mobile.ui.theme.DarkColors
import com.zheting.mobile.ui.theme.LightColors

/**
 * 全局主题：固定品牌色（哲听红），Apple Music 参考要求品牌色不随系统取色漂移，
 * 深浅色各一套；字体/圆角用统一令牌（AppTypography / AppShapes）。
 * 动态取色（Android 12+ 曾用）在 Loop 3 移除，换取视觉一致性。
 */
@Composable
fun ZTMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}