package com.zheting.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.zheting.mobile.feature.session.SessionScreen
import com.zheting.mobile.playback.PlaybackBridge
import com.zheting.mobile.playback.PlaybackLauncher
import com.zheting.mobile.playback.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow

object Destinations {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val ACCOUNT = "account"
    const val ARG_PLAYLIST_ID = "playlistId"
    const val PLAYLIST_DETAIL = "playlist/{$ARG_PLAYLIST_ID}"
    fun playlistDetail(playlistId: String) = "playlist/$playlistId"
}

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Destinations.HOME, "首页", Icons.Default.Home),
    BottomDestination(Destinations.LIBRARY, "资料库", Icons.Default.LibraryMusic),
    BottomDestination(Destinations.SEARCH, "搜索", Icons.Default.Search),
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainShell() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val miniHeight = with(LocalDensity.current) {
        (MaterialTheme.typography.titleMedium.lineHeight.toDp() +
            MaterialTheme.typography.bodyMedium.lineHeight.toDp() + 24.dp).coerceAtLeast(72.dp)
    }
    val playController by PlaybackBridge.controllerFlow.collectAsStateWithLifecycle()
    val idleState = remember { MutableStateFlow(PlaybackUiState()) }
    val playerState by (playController?.uiState ?: idleState).collectAsStateWithLifecycle()
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var queueVisible by rememberSaveable { mutableStateOf(false) }
    val expandPlayer = { focus.clearFocus(); playerExpanded = true }
    val openAccount = { navController.navigate(Destinations.ACCOUNT) { launchSingleTop = true } }
    val onSongClick: (List<Song>, Int) -> Unit = { songs, index ->
        focus.clearFocus()
        PlaybackLauncher.play(context, songs, index)
        queueVisible = false
    }
    LaunchedEffect(playerState.hasTarget) {
        if (!playerState.hasTarget) playerExpanded = false
    }

    SharedTransitionLayout {
        val sharedScope = this
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = if (playerExpanded || sharedScope.isTransitionActive) Modifier.clearAndSetSemantics {} else Modifier,
                bottomBar = {
                    Column {
                        // 保留 Mini Player 的布局空间，展开时底下的页面不跳动。
                        if (playerState.hasTarget) {
                            Box(Modifier.fillMaxWidth().height(miniHeight).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                // 避免捕获外层 ColumnScope 的 AnimatedVisibility 重载。
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !playerExpanded,
                                    enter = fadeIn(tween(180)), exit = fadeOut(tween(120)),
                                ) mini@{
                                    MiniPlayer(
                                        state = playerState,
                                        onExpand = expandPlayer,
                                        onTogglePlay = { playController?.togglePlay() },
                                        onNext = { playController?.next() },
                                        modifier = with(sharedScope) {
                                            Modifier.sharedBounds(
                                                rememberSharedContentState("player-container"), this@mini,
                                                boundsTransform = { _, _ -> spring(dampingRatio = 1f, stiffness = 400f) },
                                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                                            )
                                        },
                                        artworkModifier = with(sharedScope) {
                                            Modifier.sharedElement(
                                                rememberSharedContentState("player-artwork"), this@mini,
                                                boundsTransform = { _, _ -> spring(dampingRatio = 1f, stiffness = 400f) },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                            val backStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = backStackEntry?.destination
                            bottomDestinations.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(dest.icon, null) },
                                    label = { Text(dest.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                NavHost(navController, startDestination = Destinations.HOME, modifier = Modifier.padding(innerPadding)) {
                    composable(Destinations.HOME) {
                        HomeScreen(
                            onPlaylistClick = { navController.navigate(Destinations.playlistDetail(it)) },
                            onSongClick = onSongClick, onAccount = openAccount, currentSongId = playerState.currentSong?.id,
                        )
                    }
                    composable(Destinations.SEARCH) {
                        SearchScreen(onSongClick = onSongClick, currentSongId = playerState.currentSong?.id)
                    }
                    composable(Destinations.LIBRARY) {
                        LibraryScreen(
                            currentSong = playerState.currentSong, queueCount = playerState.queue.size,
                            onAccount = openAccount, onQueue = { queueVisible = true }, onPlayer = expandPlayer,
                            onExplore = { navController.navigate(Destinations.HOME) { launchSingleTop = true; popUpTo(Destinations.HOME) } },
                        )
                    }
                    composable(Destinations.ACCOUNT) { SessionScreen(onBack = { navController.popBackStack() }) }
                    composable(Destinations.PLAYLIST_DETAIL,
                        arguments = listOf(navArgument(Destinations.ARG_PLAYLIST_ID) { type = NavType.StringType })) { entry ->
                        PlaylistDetailScreen(
                            playlistId = entry.arguments?.getString(Destinations.ARG_PLAYLIST_ID).orEmpty(),
                            onBack = { navController.popBackStack() },
                            onSongClick = onSongClick, currentSongId = playerState.currentSong?.id,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = playerExpanded && playerState.hasTarget,
                enter = fadeIn(tween(180)), exit = fadeOut(tween(160)),
            ) full@{
                Box(Modifier.fillMaxSize()) {
                    // 截住播放器空白处的点击，防止触发下面的列表或导航。
                    Box(Modifier.fillMaxSize().clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {},
                    ).clearAndSetSemantics {})
                    FullPlayerScreen(
                        state = playerState,
                        onTogglePlay = { playController?.togglePlay() },
                        onNext = { playController?.next() },
                        onPrevious = { playController?.previous() },
                        onSeek = { playController?.seekTo(it) },
                        onQueue = { queueVisible = true },
                        onClose = { playerExpanded = false; queueVisible = false },
                        modifier = with(sharedScope) {
                            Modifier.sharedBounds(
                                rememberSharedContentState("player-container"), this@full,
                                boundsTransform = { _, _ -> spring(dampingRatio = 1f, stiffness = 400f) },
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                            )
                        },
                        artworkModifier = with(sharedScope) {
                            Modifier.sharedElement(
                                rememberSharedContentState("player-artwork"), this@full,
                                boundsTransform = { _, _ -> spring(dampingRatio = 1f, stiffness = 400f) },
                            )
                        },
                    )
                }
            }
            if (queueVisible) {
                PlayerQueueSheet(playerState, onPlayAt = { playController?.playAt(it) }, onDismiss = { queueVisible = false })
            }
        }
    }
}
