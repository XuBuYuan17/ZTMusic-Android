package com.zheting.mobile.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zheting.mobile.ui.components.EmptyView
import com.zheting.mobile.ui.components.PageTitle

/**
 * 我的（资料库）。当前为「数据未接入」占位——登录功能不在本轮范围；
 * 真实内容由 Loop 4 / Loop 9 接入。
 */
@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        PageTitle(text = "我的")
        EmptyView(
            title = "个人资料与歌单资料库尚未接入",
            message = "登录与资料库功能在后续阶段接入。",
        )
    }
}