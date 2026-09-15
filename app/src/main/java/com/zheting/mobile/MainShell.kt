package com.zheting.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zheting.mobile.core.model.Song
import com.zheting.mobile.feature.home.HomeScreen
import com.zheting.mobile.feature.library.LibraryScreen
import com.zheting.mobile.feature.player.FullPlayerScreen
import com.zheting.mobile.feature.player.MiniPlayer
import com.zheting.mobile.feature.player.PlayerQueueSheet
import com.zheting.mobile.feature.playlist.PlaylistDetailScreen
import com.zheting.mobile.feature.search.SearchScreen
import com.zheting.mobile.playback.PlaybackBridge
import com.zheting.mobile.playback.PlaybackLauncher
import com.zheting.mobile.playback.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow

/** 路由表：底部导航三入口 + 歌单详情。 */
object Destinations {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"

    const val ARG_PLAYLIST_ID = "playlistId"
    const val PLAYLIST_DETAIL = "playlist/{$ARG_PLAYLIST_ID}"

    /** 组装跳转歌单详情的实际路由。 */
    fun playlistDetail(playlistId: String) = "playlist/$playlistId"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Destinations.HOME, "首页", Icons.Filled.Home),
    BottomDestination(Destinations.SEARCH, "搜索", Icons.Filled.Search),
    BottomDestination(Destinations.LIBRARY, "我的", Icons.Filled.Person),
)

/**
 * 应用主壳：底部三 tab + 歌单详情导航 + 播放器层。
 * - 播放器控制器唯一实例在 PlaybackService，页面只读它的 StateFlow；切页面不重建、不重载；
 * - Mini Player 常驻底栏上方，点击展开全屏播放器（本轮为简单淡入过渡，Loop 7 换连续动画）；
 * - 全屏/队列同为覆盖层，不经导航栈，播放状态不随页面生命周期变化。
 */
@Composable
fun MainShell() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 播放器状态：控制器未启动时用空态兜底（列表仍可点歌，点歌即拉起服务）
    val playController by PlaybackBridge.controllerFlow.collectAsState()
    val idleState = remember { MutableStateFlow(PlaybackUiState()) }
    val playerState by (playController?.uiState ?: idleState).collectAsState()

    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var queueVisible by rememberSaveable { mutableStateOf(false) }

    val onSongClick: (List<Song>, Int) -> Unit = { songs, index ->
        PlaybackLauncher.play(context, songs, index)
        // 整队更换后回到收起态，避免旧展开叠在新队列上（连续点击不堆叠）
        queueVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    AnimatedVisibility(visible = playerState.hasTarget && !playerExpanded) {
                        MiniPlayer(
                            state = playerState,
                            onExpand = { playerExpanded = true },
                            onTogglePlay = { playController?.togglePlay() },
                            onNext = { playController?.next() },
                        )
                    }
                    NavigationBar(
                        // 克制材质：底栏用容器层色，不再另起重色块
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                    ) {
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = backStackEntry?.destination
                        bottomDestinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destinations.HOME,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Destinations.HOME) {
                    HomeScreen(
                        onPlaylistClick = { id ->
                            navController.navigate(Destinations.playlistDetail(id))
                        },
                        onSongClick = onSongClick,
                    )
                }
                composable(Destinations.SEARCH) {
                    SearchScreen(onSongClick = onSongClick)
                }
                composable(Destinations.LIBRARY) { LibraryScreen() }
                composable(
                    route = Destinations.PLAYLIST_DETAIL,
                    arguments = listOf(navArgument(Destinations.ARG_PLAYLIST_ID) { type = NavType.StringType }),
                ) { entry ->
                    val id = entry.arguments?.getString(Destinations.ARG_PLAYLIST_ID).orEmpty()
                    PlaylistDetailScreen(
                        playlistId = id,
                        onBack = { navController.popBackStack() },
                        onSongClick = onSongClick,
                    )
                }
            }
        }

        if (playerExpanded && playerState.hasTarget) {
            FullPlayerScreen(
                state = playerState,
                onTogglePlay = { playController?.togglePlay() },
                onNext = { playController?.next() },
                onPrevious = { playController?.previous() },
                onSeek = { playController?.seekTo(it) },
                onQueue = { queueVisible = true },
                onClose = {
                    playerExpanded = false
                    queueVisible = false
                },
            )
        }

        if (queueVisible && playerState.hasTarget) {
            PlayerQueueSheet(
                state = playerState,
                onPlayAt = { playController?.playAt(it) },
                onDismiss = { queueVisible = false },
            )
        }
    }
}