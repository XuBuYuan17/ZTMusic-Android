package com.zheting.mobile.feature.player

internal fun shouldDismissPlayer(distanceDp: Float, heightDp: Float, velocityDpPerSecond: Float): Boolean {
    val threshold = (heightDp * 0.2f).coerceIn(96f, 180f)
    return distanceDp >= threshold || (distanceDp >= 24f && velocityDpPerSecond >= 900f)
}
