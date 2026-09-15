package com.zheting.mobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** 品牌色：哲听红（对应原项目主色）。 */
val ZhetingRed = Color(0xFFD43C33)
val ZhetingRedDark = Color(0xFFE7665D)

/** 主题分色：Apple Music 风格暖底 + 克制红。动态取色开关见 ZTMusicTheme。 */
internal val LightColors = lightColorScheme(
    primary = ZhetingRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF3E0000),
    secondary = Color(0xFF77574E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF2C150E),
    tertiary = Color(0xFF6D5C2F),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDD8),
    onSurfaceVariant = Color(0xFF534341),
    surfaceContainerLow = Color(0xFFFFF1EE),
    surfaceContainer = Color(0xFFFAEBE7),
    surfaceContainerHigh = Color(0xFFF4E5E1),
    surfaceContainerHighest = Color(0xFFEFDFDB),
)

internal val DarkColors = darkColorScheme(
    primary = ZhetingRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB1),
    onSecondary = Color(0xFF442A1F),
    secondaryContainer = Color(0xFF5D4034),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFE6D5A0),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF1A1211),
    onBackground = Color(0xFFF0DFDC),
    surface = Color(0xFF1A1211),
    onSurface = Color(0xFFF0DFDC),
    surfaceVariant = Color(0xFF524443),
    onSurfaceVariant = Color(0xFFD7C2BE),
    surfaceContainerLow = Color(0xFF231B1A),
    surfaceContainer = Color(0xFF27201F),
    surfaceContainerHigh = Color(0xFF322A29),
    surfaceContainerHighest = Color(0xFF3D3534),
)