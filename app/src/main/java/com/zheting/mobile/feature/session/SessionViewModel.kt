package com.zheting.mobile.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zheting.mobile.ZTMusicApp
import com.zheting.mobile.core.common.ApiException
import com.zheting.mobile.core.model.AuthUser
import com.zheting.mobile.core.model.SessionStatus
import com.zheting.mobile.data.repository.SessionRepository
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 会话自检页状态。ViewModel 不持有 Context，仓库经工厂从 Application 注入。
 */
sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object NotLoggedIn : SessionUiState
    data class LoggedIn(val user: AuthUser) : SessionUiState
    data class Failed(val message: String) : SessionUiState
}

class SessionViewModel(private val repository: SessionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            _uiState.value = when (val status = repository.checkLoginStatus()) {
                is SessionStatus.NotLoggedIn -> SessionUiState.NotLoggedIn
                is SessionStatus.LoggedIn -> SessionUiState.LoggedIn(status.user)
                is SessionStatus.Error -> SessionUiState.Failed(
                    when (val e = status.throwable) {
                        is ApiException -> "${e.code}: ${e.message}"
                        else -> e.message ?: "未知网络错误"
                    },
                )
            }
        }
    }

    val apiBase: String get() = repository.apiBase

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZTMusicApp
                SessionViewModel(app.container.sessionRepository)
            }
        }
    }
}