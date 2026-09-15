package com.zheting.mobile

import android.content.Context
import com.zheting.mobile.core.network.NetworkModule
import com.zheting.mobile.core.network.NeteaseApi
import com.zheting.mobile.core.network.SessionManager
import com.zheting.mobile.core.storage.PreferenceStore
import com.zheting.mobile.data.repository.HomeRepository
import com.zheting.mobile.data.repository.PlaylistRepository
import com.zheting.mobile.data.repository.SearchRepository
import com.zheting.mobile.data.repository.SessionRepository
import com.zheting.mobile.data.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 手动装配图（约束：本阶段不引入 Hilt/Koin）。
 * 只实例化一次，随后经 ViewModelFactory 注入各 Feature。
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val preferenceStore = PreferenceStore(context)
    val sessionManager = SessionManager(preferenceStore, appScope)
    val api: NeteaseApi = NetworkModule
        .retrofit(NetworkModule.okHttpClient(sessionManager))
        .create(NeteaseApi::class.java)

    val sessionRepository = SessionRepository(api, sessionManager)
    val homeRepository = HomeRepository(api)
    val searchRepository = SearchRepository(api)
    val songRepository = SongRepository(api)
    val playlistRepository = PlaylistRepository(api, songRepository)

    /** 进程启动早期调用，恢复持久化会话。 */
    suspend fun init() {
        sessionManager.init()
    }
}