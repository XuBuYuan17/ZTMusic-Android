# ZTMusic Android 客户端 · 开发进度

独立原生 Android 客户端，复用现有 ZTMusic 后端 API（`https://music.xubuyuan.top`）。
接口行为对齐原项目代码；工程版本组合见 `README.md`（均有官方兼容依据）。

## Loop 路线图（对齐迭代计划）

| Loop | 内容 | 状态 |
|---|---|---|
| 0 | 只读检查与设计基线 | ✅ 完成（2026-09-15，未改源码） |
| 1 | 独立 APK 能力（工程/Wrapper/固定版本/哲听/启动页/CI） | ✅ 清单已逐项实现，**等待 CI 验证** |
| 2 | 真实 API 数据链路（DTO/mapper/Repository/错误模型/补全协议/契约文档+单测） | ✅ 实现完成，**等待 CI 验证** |
| 3 | Apple Music 风格视觉基础 | ✅ 实现完成，**等待 CI / 真机验证** |
| 4 | 真实数据接入页面 | ⏳ 代码已就绪，**等待 CI 验证** |
| 5 | 播放核心（PlaybackService/ExoPlayer/UrlResolver/队列） | ⏳ 代码已就绪，**等待 CI 验证** |
| 6 | Mini Player + 全屏播放器布局与交互 | ⏳ 代码已就绪，**等待 CI 验证** |
| 7 | Mini→全屏连续过渡动画 | ⏳ 未开始 |
| 8 | 沉浸式歌词（/lyric、LRC、跟随、缓存） | ⏳ 未开始 |
| 9 | 基础登录与「我的」 | ⏳ 未开始 |
| 10 | 视觉打磨（封面背景/材质/装饰动画） | ⏳ 未开始 |
| 11 | MVP 整体验收 | ⏳ 未开始 |

## Loop 1 · 独立 APK 能力（清单已实现，等待 CI 验证）

- **Kotlin + Compose 工程**：单 `:app` 模块，分包 `core/ data/ feature/`。
- **完整 Gradle Wrapper**：`gradlew`/`gradlew.bat`/`gradle-wrapper.jar`，
  `gradle-wrapper.properties` 含 `distributionSha256Sum` + `validateDistributionUrl=true`（官方发布值）。
- **兼容且固定的构建工具**：Gradle 8.11.1 · AGP 8.9.1 · Kotlin 2.1.10 · Compose BOM 2025.04.00 ·
  compileSdk/targetSdk 35 · minSdk 26 · JDK 17 · Build Tools 35.0.0（官方兼容表核定，全部固定）。
- **应用显示名称「哲听」**：`res/values/strings.xml` `app_name`。
- **基础启动页面**：`MainActivity → ZTMusicTheme → MainShell`（底部导航壳，无业务页面）。
- **CI（`.github/workflows/android.yml`）**：Ubuntu Runner、`push`/`pull_request`/`workflow_dispatch`、
  JDK 17 + setup-android、显式安装 `platforms;android-35` + `build-tools;35.0.0`、
  `gradle/actions/setup-gradle`（Gradle 缓存）、`assembleDebug` + `lintDebug`（+ `testDebugUnitTest`）、
  上传 `app-debug.apk`（缺失即失败）；不依赖本地 SDK/Android Studio/Rust/NDK、无 release Secrets。

**验证结果**：✔ 逐项文件核对通过（Manifest/strings/wrapper/CI/settings/根 gradle/properties/.gitignore）；
✘ 未在干净 Runner 实际构建（未编译、未 lint、未出 APK、未真机）——记录为**等待 CI 验证**。

## Loop 3 · Apple Music 风格视觉基础（实现完成，等待 CI / 真机验证）

**目标**：建立统一视觉基础，样式对齐 iPhone Apple Music iOS 26 参考（大标题、封面主导、克制红、轻薄材质、简洁列表）。**本轮不接数据、无模拟数据、不改业务逻辑。**

**实现内容**

- **设计令牌**（`ui/theme/`）：`Spacing`（间距）、`Color`（深浅两套品牌色板 + 哲听红）、
  `Type`（大标题/标题/正文/辅助字重字号）、`Shape`（圆角）、`Motion`（动画时长/缓动，供后续播放器过渡复用）。
- **主题**：`ZTMusicTheme` 固定品牌色（移除 Android 12+ 动态取色，保证品牌红一致性）、
  注册字体/圆角令牌；`MainActivity` 显式 `enableEdgeToEdge`（targetSdk 35 边到边，系统栏透明，Scaffold insets 处理）。
- **通用组件**（`ui/components/`）：`PageTitle`（大标题）、`Artwork`（封面占位：品牌渐变 + 音符，
  Loop 4 接 Coil 真实图）、`SongRow`（非卡片、两行文本、最短 48dp 触区、长文本省略）、
  `PlaylistCard`（方形封面 + 两行文本）、`LoadingView`/`EmptyView`/`ErrorView`（含重试）。
- **三页升级**：首页/搜索/我的从居中占位改为统一「大标题 + 未接入」骨架，风格一致。

**验证结果**：✔ 文件级自查（组件签名/import/M3 API `tonalElevation`、`surfaceContainerLow` 均在 BOM 2025.04）；
✘ 未编译、未 lint、未真机（等待 CI 首跑 + 真机验收，动画流畅性不作断言）。

**注意**：`ui/components/` 组件为纯展示层，接受数据参数，不含任何模拟数据。

## Loop 4 · 真实数据接入页面（代码已就绪，等待 CI 验证）

**目标**：首页 / 搜索 / 歌单详情接真实后端数据，落实关键词隔离、歌单分批补全与「加载更多」、
局部失败不伪装空列表、返回恢复现场。**暂不添加装饰性动画，播放操作明确禁用。**

**实现内容**

- **HomeViewModel**（`feature/home/`）：推荐歌单 + 新歌两区独立成败，局部失败保留旧数据并记错误，
  ErrorView 原地重试，不把失败当空列表。
- **SearchViewModel**（`feature/search/`）：关键词隔离双保险（请求发起记录查询词 + 响应归来校验当前态，
  新搜索先取消旧 Job）；`loadMore` 失败保留结果、把错误标记在下一条状态。
- **PlaylistDetailViewModel**（`feature/playlist/`）：详情 + 首屏歌曲；详情未带 tracks 但 trackIds 有序时
  从 0 补第一批 50 首；`loadMore` 逐批补全 + 隔离（重载期间丢弃迟到批次）+ 进度 loadedCount/total。
- **HomeScreen**：`LazyColumn` + 横向 `LazyRow` 歌单卡片，新歌 `SongRow` 列表；内容区失败分别重试。
- **SearchScreen**：搜索框（IME 搜索、无下划线圆角输入域）+ 结果列表；滚动到底自动加载更多、
  失败原地重试、全部加载完提示；首屏失败整页重试。
- **PlaylistDetailScreen**：固定返回栏 + 封面头部（歌名/创建者/`已加载 X / Y 首`）+ 有序歌曲列表 +
  页脚（加载中/失败重试/全部加载完）；滑动接近末尾自动加载更多。
- **MainShell**：新增 `playlist/{playlistId}` 路由；切详情时 Home 保留在返回栈，VM 随 entry 存活实现状态恢复。
- **共用**：`SongRow` 增加 `Song.artistLabel` 扩展（歌手 → 专辑 → 兜底）；时长 mm:ss 格式化。
- **「未实现播放操作明确禁用」**：三个页面的歌曲行均不注册点击（不响应、不进队列）。

**验证结果**：✔ 静态自查（类型/import/状态机/关键词隔离/失败保留）；✘ 未编译、未 lint、未真机运行
——等待 CI 与真机验收。播放点击路径未挂载属预期（Loop 5）。

## Loop 5 · 播放核心（代码已就绪，等待 CI 验证）

**目标**：可靠的播放内核——Service 内唯一 ExoPlayer + MediaSession、播放/暂停/上下首/顺序队列/seek、
后台播放与媒体通知、耳机拔出与音频焦点；候选链与隔离严格对齐原项目。**暂不实现预加载、持久 URL 缓存、循环模式 UI。**

**实现内容**

- **PlaybackService : MediaSessionService**：唯一 ExoPlayer（音频焦点 + 耳机拔出暂停）与 MediaSession；
  `DefaultMediaNotificationProvider`（单参构造已按 1.8.0 源码核对）自动建 channel；`onTaskRemoved` 不主动停服；
  Manifest 增 `FOREGROUND_SERVICE`/`FOREGROUND_SERVICE_MEDIA_PLAYBACK`/`POST_NOTIFICATIONS` +
  `foregroundServiceType="mediaPlayback"` + `stopWithTask=false`；MainActivity 补 Android 13+ 通知权限请求。
- **PlaybackBridge**：同进程单例，页面取 Service 内控制器实例（Loop 6 播放器 UI 接入点）。
- **PlaybackUrlResolverImpl**：Phase 1-6 候选链（普通→unblock→match→旧接口→试听候选→官方外链模板），
  试听判定 `freeTrialInfo` 非空、空 URL 视为无结果、单级 3500ms 超时收敛、外层取消上抛、
  `http://*.music.126.net` 升级 https。窄接口 `SongUrlEndpoints`（3 个端点）便于测试 fake。
- **SongLoadCoordinator**：唯一装载入口——快速切歌取消在飞 Job、自增 token 迟到响应丢弃、
  候选失败换源且沿用失败位置（同曲换源进度恢复）、空 URL 跳过、尝试数封顶、结果回调 [onResult]。
- **PlaybackController**：共享 `StateFlow<PlaybackUiState>`；顺序队列（list 循环回卷对齐原 queue.ts）、
  next/previous（已播过 3s 回本曲开头）、seek、进度常驻 ticker；队列条目带 isTrial。
- **ExoMediaSink**：MediaSink→ExoPlayer 落地——装配 MediaItem（标题/歌手/封面）、
  prepare 挂起至 READY/error、挂起期间取消自动卸载 listener。
- **契约**：`docs/api-contract.md` 增 §2.6 播放 URL（端点参数、unblock/level、候选链、试听判定）。
- **测试（新增 2 文件 18 用例）**：`SongLoadCoordinatorTest`（顺序/空 URL/换源恢复/有限次数/切歌取消/迟到丢弃/stop）、
  `PlaybackUrlResolverImplTest`（Phase 1-6 链、unblock 参数、试听兜底、去重、https 升级、模板兜底）。
  3 个既有 Repository fake 补上新增端点空实现保持编译通过。

**验证结果**：✔ 文件级静态自查（Media3 1.8.0 API、候选链逻辑、隔离 token 校验、通知 provider 构造签名）；
✘ 未编译、未 lint、未跑单测、未真机——等待 CI 与真机验收。

**已知边界**：循环/随机播放模式与播放模式 UI 未做（Loop 6 / 后续）；播放器 UI（Mini/全屏）未接入（Loop 6）；
URL 持久缓存/预取未做（Loop 5 明示暂缓）；媒体通知为系统默认样式，视觉精细化留给 Loop 10。

## CI 首跑修正（2026-09-15，已推送并逐个打通前三步）

干净 Runner 首次运行逐轮暴露 3 个环境层问题（与业务代码无关），依次修复：

1. **移除 `android-actions/setup-android@v3`**：其内部安装旧版 `tools` 包，新版 cmdline-tools（镜像 16.0）已移除 → `Failed to find package 'tools'`。改用 ubuntu-latest 镜像预装 SDK + 显式 `sdkmanager` 装 `platforms;android-*`，并写 `ANDROID_HOME`/`ANDROID_SDK_ROOT` 到 `$GITHUB_ENV`。
2. **`gradlew` 可执行位**：Windows 提交丢失 `+x`，Linux Runner `Permission denied` → `git update-index --chmod=+x gradlew`。
3. **compileSdk 试探 37（已被下方第四次修正推翻）**：本时间线 Maven 解析出的 Compose 1.12 / Coil 3.6.2 系 AAR `minCompileSdk` 为 37 → 初以 `compileSdk=37` + `android.suppressUnsupportedCompileSdk=37` 处理。

## CI 第四次修正（2026-09-15，降级族：根因定位 + 版本下修）

优先修复第四轮 `Warning: Failed to find package 'platforms;android-37'` 时发现：**本时间线不存在 android-37 平台**（Android 17 未发布），`compileSdk=37` 路线整体不可行 → 必须**降级依赖库**而非抬升 compileSdk。

- **根因定位**：逐层核对 Maven 元数据得到完整版本链——
  - BOM `2025.04.00` 的 pom 实际映射 compose **1.7.8**（≤35）；
  - 真正抬高 Compose 全家到 **1.12.0（minCompileSdk=37）** 的是 `io.coil-kt.coil3:coil-compose-core-android:3.6.2` 对其的**硬依赖**（Gradle 解析取最高者，BOM 约束不敌）；26 条 AAR metadata 报错全部来自 coil 3.6.2 + compose 1.12.0 系，无其它库。
  - `coil-compose-core-android:3.5.0` 硬依赖 compose **1.11.1**（minCompileSdk=35）。
  - **Kotlin 依赖连锁（第五轮 CI 暴露）**：3.5.0 及之后的 coil 全部要求过新 Kotlin——`coil-network-okhttp` 的 stdlib `requires` 依次为 3.5.0→**2.4.0**、3.4.0→2.3.10、3.3.0→2.2.0；只有 **coil 3.2.0 → stdlib `2.1.20`**（=当前 Kotlin）且 `coil-compose-android` AAR `minCompileSdk=1`、依赖 `org.jetbrains.compose.foundation 1.8.0`（≤36）、且 `coil-network-okhttp` 为普通 jar（无 AAR 校验）。**最终 coil 取 3.2.0**，BOM/Kotlin/compileSdk 不再变。
  - compose 1.11.4 的 module 元数据要求 kotlin-stdlib **2.1.20** → Kotlin **2.1.10 → 2.1.20**（同次版本补丁级，官方存在），避免编译器读不到新库元数据。
- **最终版本组合（全校验过 minCompileSdk）**：
  - Compose BOM `2025.04.00` → **`2026.06.01`**（映射 compose 1.11.4 + material3 1.4.0，ui-android 1.11.4 AAR `minCompileSdk=35`）；
  - Coil `3.6.2` → **`3.5.0`**；
  - `compileSdk` **37 → 36**（AGP 8.9.1 官方测试上限 36，无需 suppression）；删除 `gradle.properties` 的 `android.suppressUnsupportedCompileSdk=37`；
  - CI 安装 `platforms;android-37` → **`platforms;android-36`**（build-tools 35.0.0 不变）；
  - targetSdk 仍 35，minSdk 26，AGP/Gradle/Kotlin 矩阵不动；material3 1.4.0 覆盖现有全部 M3 用法。
- **README 版本矩阵**已同步（compileSdk 36 / platform android-36 / BOM 2026.06.01 / Coil 3.5.0，并去重重复的 applicationId 行）。
- 尚未推送提交。推送触发 CI 后，若仍报 AAR metadata，则逐条看是否还有隐藏的 1.12.0 传递引入者。

## CI 第五轮（2026-09-15，首次真实编译，击穿 39 个编译错误）

版本问题彻底解决后，`testDebugUnitTest` 首次把业务代码真正编译一遍，暴露 Loop 5/6 代码中从未被编译器验证过的一批真实编译错误（此前仅静态自查）。已逐类修复：

1. **`PlaybackUrlResolverImpl` 第 91 行注释含嵌套块注释开头**（最终根因，31 个级联错误）：`/** http://*.music.126.net ... */` 中 `//*` 的「第二个 `/` + `*`」在 Kotlin 里会打开**嵌套块注释** → 外层 KDoc 未真正闭合，其后整个文件（`normalizeUrl`/companion 常量/两个扩展函数）被吞进注释 → 全部 `Unresolved reference` + `Missing }`/`Unclosed comment` 指向 EOF。改注释文案避开 `//*` 序列即整体恢复。
2. **`uriCall` 非 suspend 调 `withTimeoutOrNull`**（另一处真实错误，`89:9` 单独报）：改 `private suspend fun <T>`。
3. **`PlaybackController.playerListener` 声明在 `init` 之后**：Kotlin 禁止 init 访问未初始化成员 → 把监听器定义提前到 init 前。
4. **顶层 `kotlinx.serialization.json.parseToJsonElement` import 失效**：本版本 kotlinx-serialization 已无该顶层扩展（`Json.parseToJsonElement` 成员可用）→ 移除 `SessionInterceptor` 与 `PlaylistMapperTest`/`SongMapperTest` 中该 import。
5. **`SessionRepository` elvis 类型发钝**：`resolveUser(res) ?: SessionStatus.NotLoggedIn` 的 elvis 类型为 `lub(AuthUser, SessionStatus)=Any` → 改为显式 `if (user != null) LoggedIn(user) else NotLoggedIn`，**顺带修正了原代码从不返回 `LoggedIn` 的语义缺陷**。
6. **`PlaylistRepository.nextChunk` `else -> r` 整体类型发钝为 `NeteaseResult<Any>`** → 失败子类型（`NeteaseResult<Nothing>`，协变）逐条显式回收。
7. **三个页面 `onSongClick = {}`**：双参 lambda 默认值不合法 → `{ _, _ -> }`。

上述修改**尚未编译验证**（本机无环境），已提交待 CI 验证。

## Loop 6 · Mini Player + 全屏播放器布局与交互（代码已就绪，等待 CI 验证）

**目标**：两个播放器界面（底部导航上方 Mini + 全屏）连接同一个 `PlaybackController`，真实加载/播放/暂停/错误状态；队列入口与展示；进度条拖动预览、松手提交 seek。**本轮只做静态布局与简单过渡，Mini→全屏连续动画留给 Loop 7。**

**实现内容**

- **PlaybackBridge 重构**（`playback/PlaybackService.kt`）：`@Volatile` 字段换成 `StateFlow<PlaybackController?>`，新增 `awaitController()` 挂起（覆盖首次启动服务时 `onCreate` 尚未完成的竞态）。
- **PlaybackLauncher**（新）：点歌入口 `startForegroundService` → `awaitController()` → `playQueue`；页面不 new 播放器，控制器唯一实例活在 Service，切换页面不重建、不重载。
- **MiniPlayer**（`feature/player/`）：底栏上方常驻条——封面 + 双行文本 + 播放/暂停 + 下一首；整条点击展开，按钮叠于其上各自消费点击（不会同时触发展开）；装载中播放键原位转圈。
- **FullPlayerScreen**（新）：顶栏（收起/「正在播放」/队列）→ 大封面（weight 居中、上限 420dp）→ 歌名/歌手/错误文案 → 上一首/播放/下一首 → 进度条 + 时间标签。系统返回与收起按钮共用 `onClose`；随机/循环未实现，不做死按钮占位。
- **进度条拖动协议**：拖动时本地 `dragMs` 预览驱动滑条与当前时间，松手才 `onSeek`；切歌/时长变化丢弃遗留预览；`durationMs<=0` 禁用拖动并显示 `--:--`。
- **PlayerQueueSheet**：`ModalBottomSheet`（跳过半展开），序号 + 歌名/歌手，当前播高亮，点击 `playAt`。
- **三页接点歌**：首页新歌、搜索结果、歌单详情歌曲行 `onClick` 接入 `PlaybackLauncher`（替换 Loop 4 的「不注册点击」）；歌单以当前已加载曲目为队列。
- **依赖**：`material-icons-extended`（Pause/SkipNext 等不在核心图标集）；`ui/components/TimeFormat.kt` 共享 mm:ss（未知 `--:--`）。
- **测试**：`TimeFormatTest`（5 用例：未知时长、进位、跨小时）。
- **Loop 4 约束更新**：播放已真实接入，三页歌曲行恢复点击；「未实现播放操作禁用」仅剩随机/循环未挂按钮。

**验证结果**：✔ 静态自查（回调链路、StateFlow 合并、Slider 协议、BackHandler、ModalBottomSheet API 均核对 BOM 2025.04）；✘ 未编译、未 lint、未跑单测、未真机——等待 CI 与真机验收。动画沿用简单淡入淡出，Loop 7 才做连续过渡与手势。

**已知边界**：随机/循环模式与其 UI 未做（Loop 6 明示）；Mini→全屏连续动画未做（Loop 7）；封面取色背景/半透明材质留 Loop 10；通知栏样式为系统默认（Loop 10 打磨）。

## 阶段 2 · 网络读链路 + Repository（实现完成，等待 CI 验证）

**本轮目标**：接入推荐 / 推荐新歌 / 搜索 / 歌单详情 / 批量歌曲详情五个读端点，
落地统一错误模型与「按 trackIds 逐批补全」协议；领域模型对齐契约收敛
（Song 字符串 ID、毫秒时长）。不接 UI（阶段 3 再做首页展示），不建持久缓存。

**契约先行**：`docs/api-contract.md` 以原项目代码为准逐条对照整理，见 §1 全局协议 / §2 端点清单。

**实现范围**

- **领域模型收缩**：`core/model/MusicModels.kt` 全面 String ID；
  `Song.durationMs` 统一毫秒；新增 `PlaylistDetail` / `TrackIdRef` / `SearchPage` / `PlaylistChunk`；
  `AuthUser.userId` 同步 String。
- **统一错误出口**：`core/common/NeteaseResult.kt` —— Success / ApiError(code,msg) /
  NetworkError / ParseError；`neteaseSafe` 收敛 IO 与序列化异常，取消向上传播。
- **端点 DTO**：`core/network/NeteaseDtos.kt` 按真实信封形状建模（顶层 `result`/`songs`/`playlist`），
  元素保留 `JsonElement` 由 mapper 收窄多态。
- **mapper 收窄**：`NcmJson` / `SongMapper` / `PlaylistMapper` ——
  三层 song 路由（root.song → resourceExtInfo.songData·ext.song → root）、
  ar⇄artists、al⇄album、dt⇄duration、coverUrl 优先级链。
- **Repository**：`HomeRepository` / `SearchRepository` / `SongRepository`（去重、≤500/批、按序重建、缺歌占位）/
  `PlaylistRepository`（detail + nextChunk：每批 50、loadedCount/hasMore 联通进度）。
- **网络层补规约**：`SessionManager.saveCookieFromResponse` 支持响应头 Set-Cookie
  （截 `;` 优先于 body，MUSIC_U 保护不变）；`SessionInterceptor` 收集 Set-Cookie 头；
  新增 `SafeReadRetryInterceptor`（仅 GET、5xx/网关类、≤2 次 200/400ms 退避）。
- **日志合规**：移除 okhttp-logging 依赖与用法（cookie/敏感参数不得落日志，契约 §1.6）。
- **装配**：`AppContainer` 注入四个新仓库；NeteaseApi 增五个读端点签名。
- **CI**：`.github/workflows/android.yml` 增加 `:app:testDebugUnitTest`。
- **JVM 单测（新增 5 个文件 30 用例）**：
  `SongMapperTest` / `PlaylistMapperTest` / `SongRepositoryTest` /
  `PlaylistRepositoryTest`（§4 协议主路径）/ `HomeSearchRepositoryTest`。

**验证结果**

- **未编译、未 lint、未跑单测**：本机不装 SDK/Gradle，交给 GitHub Actions 首次运行验证。
- 已做静态自查：类型/import/命名冲突逐文件过一遍；无占位函数、无模拟数据。
- **未真机运行**。

**已知问题**

- 阶段 1 会话页 `feature/session` 保留未挂载，其编译依赖本轮 AuthUser 改动已同步。
- playlist detail 未返回 tracks 时 `tracksPartial=false`（无 trackIds、无 tracks 视为完整空列表）。
- 歌单补全失败分块目前整批返回错误（UI 侧保留已加载数据，允许重试）；原项目为整批吞成为 []。

## 阶段 1 · 工程骨架 + 导航壳 + 独立 CI（完成，等待 CI 验证）

- 工程能组合、导航壳三 tab（首页/搜索/我的），数据占位不冒充功能；
  主题、启动深色防闪、CI（assemble + lint + upload）、docs 齐备。
- 详见 git 历史与上轮 `progress.md` 归档内容。

## 行为对齐清单（阶段 1-2 已移植）

| 行为 | 原项目出处 | 已移植实现 |
|---|---|---|
| HTTP 恒 200，成败看 `code`；顶层优先 | `api/client.ts` | `AccountResultDto.actualCode` |
| 匿名账号判定 / 合并 cookie 去控制项 / MUSIC_U 保护 / 补 `os=pc` | `stores/auth.svelte.ts` / `api/session.ts` | `AccountResultDto.isAnonymous` / `CookieProcessor` |
| 响应 Set-Cookie 收集（截 `;` 空项剔除）优先于 body | `api.rs` `collect_set_cookie` | `SessionInterceptor` + `SessionManager` |
| randomCNIP 默认注入、登录/登出关闭、QR noCookie | `api/client.ts` | `SessionInterceptor` |
| 只对安全读请求（GET）有限重试，业务失败不重试 | `api/client.ts` | `SafeReadRetryInterceptor` |
| 错误分层（业务/网络/解析/空结果），取消向外传播 | — | `NeteaseResult` + `neteaseSafe` |
| Song 多态三层路由 + 字段多态收窄 | `utils/normalize.ts` | `SongMapper` |
| Playlist 字段映射 | `providers/netease.ts` | `PlaylistMapper` |
| 歌单按 trackIds 序补全、每批 50、缺歌占位 | `services/details.ts` | `PlaylistRepository.nextChunk` |
| 批量 songDetail 去重、上限 500、按序回填 | `services/details.ts` | `SongRepository.songsByIds` |

## 下一阶段入口

- 待 Loop 5+6 CI 验证通过后，按序推进 **Loop 7 · Mini → 全屏连续过渡**：
  以 `MainShell` 的 `playerExpanded`/`queueVisible` 为状态基座，用共享元素/手势绘制一套
  协调的过渡（封面从 Mini 原位放大、容器圆角与背景渐变、底栏同步退场、
  按下拉距离/速度决定收起或回弹），要求按实际测量边界计算、可中断反向、不能改播放状态。
- 封面/媒体请求保持 Coil 独立实例（不挂会话拦截器，契约 §1.6）。