package com.zheting.mobile.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zheting.mobile.ZTMusicApp
import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 搜索状态。Idle 未搜索；结果页携带本次查询词（用于响应归属校对）。 */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Content(
        val query: String,
        val songs: List<Song>,
        val songCount: Int,
        val hasMore: Boolean,
        /** 首屏失败；加载更多失败时保留已有结果并把错误标记在下条状态。 */
        val error: String? = null,
    ) : SearchUiState
}

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * 搜索新关键词。
     * - 关键词隔离：请求发起时记录 [query]，响应回来若 [uiState] 当前词已变则丢弃（不覆盖）；
     * - 协程取消：新搜索先取消旧 Job，正在飞的旧请求响应直接被放弃。
     */
    fun search(keywords: String) {
        val q = keywords.trim()
        if (q.isEmpty()) {
            searchJob?.cancel()
            searchJob = null
            _uiState.value = SearchUiState.Idle
            return
        }
        // 已展示同一关键词结果时不重复搜
        val current = _uiState.value
        if (current is SearchUiState.Content && current.query == q && current.error == null) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            val res = repository.searchSongs(q)
            // 隔离：只有当前仍在 Loading 且关注词未变时才接受
            val now = _uiState.value
            if (now !is SearchUiState.Loading) return@launch
            _uiState.value = when (res) {
                is NeteaseResult.Success -> SearchUiState.Content(
                    query = q,
                    songs = res.data.songs,
                    songCount = res.data.songCount,
                    hasMore = res.data.hasMore,
                )
                is NeteaseResult.ApiError -> SearchUiState.Content(q, emptyList(), 0, false, "搜索失败 ${res.code}：${res.message}")
                is NeteaseResult.NetworkError -> SearchUiState.Content(q, emptyList(), 0, false, "网络错误，请稍后重试")
                is NeteaseResult.ParseError -> SearchUiState.Content(q, emptyList(), 0, false, "响应解析失败")
            }
        }
    }

    /** 加载下一页。失败保留已有结果并记录错误（不伪装成空）。 */
    fun loadMore() {
        val current = _uiState.value as? SearchUiState.Content ?: return
        if (!current.hasMore || current.songs.isEmpty()) return
        if (searchJob != null && searchJob?.isActive == true) return
        searchJob = viewModelScope.launch {
            // 标记加载更多进行中（error 保留原来内容）
            _uiState.value = current.copy(error = current.error)
            val offset = current.songs.size
            val res = repository.searchSongs(current.query, limit = LOAD_MORE_LIMIT, offset = offset)
            val state = _uiState.value
            if (state !is SearchUiState.Content || state.query != current.query) return@launch
            _uiState.value = when (res) {
                is NeteaseResult.Success -> state.copy(
                    songs = state.songs + res.data.songs,
                    songCount = res.data.songCount,
                    hasMore = res.data.hasMore,
                    error = null,
                )
                else -> state.copy(error = "加载更多失败，请重试")
            }
        }
    }

    companion object {
        private const val LOAD_MORE_LIMIT = 30

        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZTMusicApp
                SearchViewModel(app.container.searchRepository)
            }
        }
    }
}
