package com.zheting.mobile.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 阶段 1 会话自检页：真实请求 /login/status 判定 应用↔后端↔账号 状态。
 * 后续阶段会被真正的 Home/登录入口页面替换，功能性内容到此为止，不占位。
 */
@Composable
fun SessionScreen(
    viewModel: SessionViewModel = viewModel(factory = SessionViewModel.factory()),
) {
    LaunchedEffect(Unit) { viewModel.start() }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "哲听 · ZT Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "原生 Android 客户端 · 阶段 1",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                InfoRow("API", viewModel.apiBase)
                InfoRow("会话", when (state) {
                    is SessionUiState.Loading -> "检测中…"
                    is SessionUiState.NotLoggedIn -> "未登录"
                    is SessionUiState.LoggedIn -> "已登录"
                    is SessionUiState.Failed -> "异常"
                })
                when (val s = state) {
                    is SessionUiState.Loading -> {
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(Modifier.size(32.dp))
                    }
                    is SessionUiState.NotLoggedIn -> {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "本地无有效登录态。二维码 / 手机 / 邮箱登录在本阶段未实现，见 docs/progress.md。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is SessionUiState.LoggedIn -> {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "欢迎，${s.user.nickname}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "uid=${s.user.userId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is SessionUiState.Failed -> {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "无法连接后端或响应非法。若本机无网络代理请检查网络；错误信息为真实响应。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}