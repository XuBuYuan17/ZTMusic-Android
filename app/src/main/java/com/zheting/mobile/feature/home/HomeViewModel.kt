package com.zheting.mobile.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zheting.mobile.ZTMusicApp
import com.zheting.mobile.core.common.NeteaseResult
import com.zheting.mobile.core.model.PlaylistRef
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 首页状态：推荐歌单 + 新歌，两区块各自独立成败（局部失败不拖垮整页）。 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val playlists: List<PlaylistRef>,
        val newSongs: List<Song>,
        val playlistError: String? = null,
        val newSongsError: String? = null,
    ) : HomeUiState
}

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        load()
    }

    /** 重试/刷新（两区独立成败，失败区块保留旧数据并记错误，不把失败伪装成空列表）。 */
    fun load() {
        viewModelScope.launch {
            val prev = _uiState.value as? HomeUiState.Content
            val playlists = repository.personalized()
            val newSongs = repository.newSongs()
            _uiState.value = HomeUiState.Content(
                playlists = when (playlists) {
                    is NeteaseResult.Success -> playlists.data
                    else -> prev?.playlists.orEmpty()
                },
                newSongs = when (newSongs) {
                    is NeteaseResult.Success -> newSongs.data
                    else -> prev?.newSongs.orEmpty()
                },
                playlistError = (playlists as? NeteaseResult.ApiError)?.let { "${it.code}: ${it.message}" }
                    ?: (playlists as? NeteaseResult.NetworkError)?.let { "歌单加载失败：网络错误" }
                    ?: (playlists as? NeteaseResult.ParseError)?.let { "歌单解析失败" },
                newSongsError = (newSongs as? NeteaseResult.ApiError)?.let { "${it.code}: ${it.message}" }
                    ?: (newSongs as? NeteaseResult.NetworkError)?.let { "新歌加载失败：网络错误" }
                    ?: (newSongs as? NeteaseResult.ParseError)?.let { "新歌解析失败" },
            )
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZTMusicApp
                HomeViewModel(app.container.homeRepository)
            }
        }
    }
}