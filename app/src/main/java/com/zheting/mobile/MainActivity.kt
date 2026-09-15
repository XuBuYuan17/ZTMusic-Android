package com.zheting.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat

/**
 * 界面入口：装载主题与主壳（底部导航）。
 * enableEdgeToEdge：targetSdk 35 系统强制边到边，显式声明保证低版本一致，
 * 状态栏/导航栏透明 + 图标色自动随深浅色（系统栏由 Scaffold insets 处理）。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            ZTMusicTheme {
                MainShell()
            }
        }
    }

    /** Android 13+：媒体通知需显式授权，不弹窗则通知不展示（不影响播放）。 */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION)
    }

    private companion object {
        const val REQ_NOTIFICATION = 100
    }
}