package com.zheting.mobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** 品牌色：哲听红（对应原项目主色）。 */
val ZhetingRed = Color(0xFFE83C58)
val ZhetingRedDark = Color(0xFFFF5B75)

/** 中性背景把色彩留给封面，强调色只用于操作与选中态。 */
internal val LightColors = lightColorScheme(
    primary = ZhetingRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF3E0000),
    secondary = Color(0xFF63636B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECECF0),
    onSecondaryContainer = Color(0xFF2C150E),
    tertiary = Color(0xFF6D5C2F),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFCFCFD),
    onBackground = Color(0xFF18181B),
    surface = Color(0xFFFCFCFD),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFECECF0),
    onSurfaceVariant = Color(0xFF74747D),
    surfaceContainerLow = Color(0xFFF7F7F9),
    surfaceContainer = Color(0xFFF0F0F3),
    surfaceContainerHigh = Color(0xFFECECF0),
    surfaceContainerHighest = Color(0xFFE2E2E8),
    outline = Color(0xFF92929B),
    outlineVariant = Color(0xFFE2E2E8),
)

internal val DarkColors = darkColorScheme(
    primary = ZhetingRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFBDBDC5),
    onSecondary = Color(0xFF442A1F),
    secondaryContainer = Color(0xFF303036),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFE6D5A0),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF101012),
    onBackground = Color(0xFFF4F4F6),
    surface = Color(0xFF101012),
    onSurface = Color(0xFFF4F4F6),
    surfaceVariant = Color(0xFF36363D),
    onSurfaceVariant = Color(0xFFABABB5),
    surfaceContainerLow = Color(0xFF19191D),
    surfaceContainer = Color(0xFF242429),
    surfaceContainerHigh = Color(0xFF2D2D33),
    surfaceContainerHighest = Color(0xFF393940),
    outline = Color(0xFF85858F),
    outlineVariant = Color(0xFF34343B),
)
