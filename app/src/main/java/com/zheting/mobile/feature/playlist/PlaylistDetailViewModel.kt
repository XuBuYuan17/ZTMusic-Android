package com.zheting.mobile.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zheting.mobile.ZTMusicApp
import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.model.PlaylistDetail
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.data.repository.PlaylistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 歌单详情状态。Content 维护已补全歌曲与进度（loadedCount/total）。 */
sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Error(val message: String) : PlaylistDetailUiState
    data class Content(
        val detail: PlaylistDetail,
        val songs: List<Song>,
        /** 已加载到 trackIds 的进度下标（含首屏 tracks）。 */
        val loadedCount: Int,
        val total: Int,
        val hasMore: Boolean,
        val loadingMore: Boolean = false,
        /** 加载更多失败原因；首屏歌曲失败另作错误标记。 */
        val loadMoreError: String? = null,
    ) : PlaylistDetailUiState
}

class PlaylistDetailViewModel(
    private val repository: PlaylistRepository,
    private val playlistId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private var started = false
    private var loadMoreJob: Job? = null

    /** 详情 + 首屏歌曲只加载一次；重试走 [load]。 */
    fun start() {
        if (started) return
        started = true
        load()
    }

    /**
     * 加载详情。首屏补齐规则：
     * - 详情自带 tracks 且覆盖 trackIds → 直接用，无需再补；
     * - 详情 tracks 为空但 trackIds 有序 → 从 0 补第一批 50 首，缺失以占位呈现；
     * - 补第一批失败不伪装成空列表，标记 loadMoreError 供原地重试。
     */
    fun load() {
        viewModelScope.launch {
            _uiState.value = PlaylistDetailUiState.Loading
            val res = repository.detail(playlistId)
            _uiState.value = when (res) {
                is NeteaseResult.Success -> toContent(res.data)
                is NeteaseResult.ApiError -> PlaylistDetailUiState.Error("加载失败 ${res.code}：${res.message}")
                is NeteaseResult.NetworkError -> PlaylistDetailUiState.Error("网络错误，请稍后重试")
                is NeteaseResult.ParseError -> PlaylistDetailUiState.Error("响应解析失败")
            }
        }
    }

    private suspend fun toContent(detail: PlaylistDetail): PlaylistDetailUiState.Content {
        val total = detail.trackIds.size.takeIf { it > 0 }
            ?: detail.trackCount.takeIf { it > 0 }
            ?: detail.tracks.size
        var songs = detail.tracks
        var loaded = detail.tracks.size
        var hasMore = loaded < total && detail.trackIds.isNotEmpty()
        var firstChunkError: String? = null

        // 详情未带首屏歌曲但有序 trackIds 齐备：从 0 补第一批
        if (detail.tracks.isEmpty() && detail.trackIds.isNotEmpty()) {
            when (val chunk = repository.nextChunk(detail, 0)) {
                is NeteaseResult.Success -> {
                    songs = chunk.data.songs
                    loaded = chunk.data.loadedCount
                    hasMore = chunk.data.hasMore
                }
                is NeteaseResult.ApiError ->
                    firstChunkError = "加载失败 ${chunk.code}：${chunk.message}"
                is NeteaseResult.NetworkError -> firstChunkError = "网络错误，请稍后重试"
                is NeteaseResult.ParseError -> firstChunkError = "响应解析失败"
            }
        }
        return PlaylistDetailUiState.Content(
            detail = detail,
            songs = songs,
            loadedCount = loaded,
            total = total,
            hasMore = hasMore,
            loadMoreError = firstChunkError,
        )
    }

    /** 加载下一批。失败保留已有结果并标记错误（不伪装成空）。 */
    fun loadMore() {
        val current = _uiState.value as? PlaylistDetailUiState.Content ?: return
        if (!current.hasMore || current.loadingMore) return
        if (loadMoreJob != null && loadMoreJob?.isActive == true) return
        loadMoreJob = viewModelScope.launch {
            val pending = current.copy(loadingMore = true, loadMoreError = null)
            _uiState.value = pending
            when (val res = repository.nextChunk(current.detail, current.loadedCount)) {
                is NeteaseResult.Success -> {
                    val now = _uiState.value
                    // 隔离：期间如果重载了详情则丢弃本批
                    if (now !is PlaylistDetailUiState.Content || now.detail.id != current.detail.id) return@launch
                    _uiState.value = now.copy(
                        songs = now.songs + res.data.songs,
                        loadedCount = res.data.loadedCount,
                        hasMore = res.data.hasMore,
                        loadingMore = false,
                    )
                }
                else -> {
                    val now = _uiState.value as? PlaylistDetailUiState.Content ?: return@launch
                    _uiState.value = now.copy(loadingMore = false, loadMoreError = "加载更多失败，请重试")
                }
            }
        }
    }

    companion object {
        fun factory(playlistId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZTMusicApp
                PlaylistDetailViewModel(app.container.playlistRepository, playlistId)
            }
        }
    }
}