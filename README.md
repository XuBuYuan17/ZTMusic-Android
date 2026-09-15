# ZTMusic Android（哲听 · Android）

独立原生 Android 客户端，复用现有 ZTMusic 后端 API（`https://music.xubuyuan.top`）。
Kotlin + Jetpack Compose + Material 3。当前处于阶段 1：工程骨架 + 底部导航壳。

> 本仓库**不包含**原 ZTMusic 前端/后端代码，仅作为独立客户端存在。
> 音乐数据来自第三方 API，仅供个人学习与技术交流。

## 技术栈与版本（固定，均有官方兼容依据）

| 组件 | 版本 | 依据 |
|---|---|---|
| Gradle | 8.11.1 | [AGP 8.9 官方兼容表](https://developer.android.com/build/releases/agp-8-9-0-release-notes) 的最低/默认版本 |
| Android Gradle Plugin | 8.9.1 | 同上，上限 compileSdk 35 |
| JDK | 17 | 同上（最小要求） |
| Build Tools / Platform | 35.0.0 / android-35 | 同上 |
| Kotlin | 2.1.10 | Kotlin 稳定版 |
| Jetpack Compose BOM | 2025.04.00 | [Compose↔Kotlin 兼容映射](https://developer.android.com/jetpack/compose/kotlin)：Kotlin 2.1.10 |
| Navigation Compose | 2.8.9 | androidx 稳定版 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 | 与 AGP 8.9 上限一致 |
| applicationId | `com.zheting.mobile` | 与原 Tauri Android 版（`zheting`）独立，可并存安装 |

版本号由 `gradle/libs.versions.toml` 统一管理，禁止动态版本。

## 目录结构

```
app/                    单 app 模块
├─ src/main/java/com/zheting/mobile/
│  ├─ MainActivity.kt   界面入口
│  ├─ MainShell.kt      底部导航壳（首页 / 搜索 / 我的）
│  ├─ ZTMusicApp.kt     Application
│  ├─ ZTMusicTheme.kt   深/浅色基础主题
│  ├─ core/             核心层（模型 / 网络 / 存储，阶段 1 保留未接入）
│  ├─ data/             数据层（repository / mapper，同上）
│  └─ feature/          功能分包（home / search / library / session）
└─ src/test/            JVM 单元测试
.github/workflows/      CI（Android CI）
docs/progress.md        开发进度
```

## 本地只编辑代码，不再装构建环境

本机**不要**安装 Android Studio / Android SDK / Gradle / Rust / NDK。构建与校验全部由 GitHub Actions 完成：

1. 编辑/新增源码与配置文件。
2. `git commit` 后 `git push`（推送前 `git add` 需要的文件）。
3. 在 GitHub 仓库 **Actions** 页查看 **Android CI** 运行结果：
   - `:app:assembleDebug`（生成 `app-debug.apk`）
   - `:app:lintDebug`（静态检查）
4. 通过后在**运行页底部 Artifacts** 下载 `app-debug`，其中包含 `app-debug.apk`。

## 真机安装

1. 下载 CI 当次运行的 `app-debug` 产物并解压得到 `app-debug.apk`。
2. 将 APK 拷贝到 Android 手机（数据线 / 网盘 / 本地服务均可）。
3. 手机上点击 APK，允许「安装未知来源应用」（Android 各版本具体位置略有差异）。
4. 安装在应用列表可见「哲听」（applicationId `com.zheting.mobile`，可与 Tauri Android 版并存）。

> debug 包使用 Android debug 签名（`~/.android/debug.keystore`），不需要 Release 签名。

## 当前状态

- **阶段 1（进行中，等待 CI 验证）**：工程骨架、底部导航壳、深/浅色基础主题、独立 CI 构建流程。
- 首页/搜索/我的三个 tab 均为「数据未接入」的真实占位，未使用任何模拟数据。
- API / 登录 / 播放器**不在本阶段范围**；仓库保留的 `core/`、`data/` 会话层源码将在后续阶段接入验证。
- 验收以 GitHub Actions 从干净 Runner 独立构建成功（assembleDebug + lintDebug 通过、APK 产物生成）为准；**未触发 CI 前一律视为"等待 CI 验证"，不宣称构建通过**。