package com.zheting.mobile.feature.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zheting.mobile.ui.components.Artwork
import com.zheting.mobile.ui.components.ErrorView
import com.zheting.mobile.ui.components.LoadingView
import com.zheting.mobile.ui.components.PageTitle

@Composable
fun SessionScreen(
    onBack: () -> Unit = {},
    viewModel: SessionViewModel = viewModel(factory = SessionViewModel.factory()),
) {
    LaunchedEffect(Unit) { viewModel.start() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        PageTitle("账户")
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val user = (state as? SessionUiState.LoggedIn)?.user
            if (user != null && user.avatarUrl.isNotBlank()) {
                Artwork(Modifier.size(96.dp), user.avatarUrl, cornerRadiusDp = 48)
            } else {
                Icon(Icons.Default.AccountCircle, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))
            when (val value = state) {
                SessionUiState.Loading -> LoadingView(message = "正在检查登录状态…")
                SessionUiState.NotLoggedIn -> {
                    Text("以游客身份听歌", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text("你可以浏览推荐、搜索和播放歌曲。\n当前版本暂未开放二维码登录。",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is SessionUiState.LoggedIn -> {
                    Text(value.user.nickname, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("已登录", color = MaterialTheme.colorScheme.primary)
                }
                is SessionUiState.Failed -> ErrorView(value.message, onRetry = viewModel::refresh)
            }
            Spacer(Modifier.height(24.dp))
            if (state !is SessionUiState.Loading && state !is SessionUiState.Failed) {
                OutlinedButton(onClick = viewModel::refresh) { Text("刷新登录状态") }
            }
        }
    }
}
