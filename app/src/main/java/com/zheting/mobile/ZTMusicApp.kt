package com.zheting.mobile

import android.app.Application
import kotlinx.coroutines.runBlocking

class ZTMusicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 恢复会话：仅读取两个偏好键，进程启动早期同步完成，第一帧即可用。
        runBlocking { container.init() }
    }
}