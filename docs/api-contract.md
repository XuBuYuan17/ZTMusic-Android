# API 契约（phase 2 · 网络层与 Repository）

> 本契约**以原项目实际代码为准**整理，字段/参数/错误语义逐条对照，
> 不凭文档猜测接口形状。契约先于实现；实现与契约不一致时以本文件为第一依据，
> 但最终以真实后端响应为准（CI/真机验证阶段反馈校正）。
>
> 对照来源：
> - `src/lib/api/client.ts` —— 端点与参数
> - `src/lib/api/session.ts` —— cookie 会话
> - `src/lib/api/cache-ttl.ts` —— 读请求缓存 TTL（Android 本阶段不实现持久缓存）
> - `src/lib/music/providers/netease.ts` / `src/lib/utils/normalize.ts` —— 字段映射
> - `src/lib/services/details.ts` —— 歌单 trackIds 补全
> - `src-tauri/src/api.rs` —— 代理行为（GET query / POST form / Set-Cookie 回传）

## 1. 全局协议

### 1.1 Base URL 与传输

- 默认 `https://music.xubuyuan.top`，可配置（`api_base`）。
- GET：参数进 query；cookie 进 query 的 `cookie` 参数（`client.ts` 浏览器链路行为）。
- POST：`application/x-www-form-urlencoded`；cookie 追加进 form 的 `cookie` 字段。
- Referer/User-Agent：后端下沉逻辑（api.rs 代理），Android 直接请求不需要模拟。

### 1.2 成败判定（信封）

- **HTTP 层恒 200**，业务成败看 body 的 `code`：`code === 200` 成功。
- **不能假设所有响应都是统一 `{code, data}`**：`/song/detail` 的歌曲在顶层 `songs`，
  `/playlist/detail` 的歌单在顶层 `playlist`，`/search` 系在 `result`，
  `/login/status` 顶层与 `data` 内都可能出现 `code`。
- **不能把 HTTP 200 等同业务成功**：`/user/account` 等接口 body 内 `code` 非 200 时按失败处理。
- 失败消息取 `message`（`msg` 兜底）。
- 部分响应无 `code` 字段（如纯数组），按成功处理并保留原行为。

### 1.3 randomCNIP

- 普通请求默认注入 `randomCNIP=true`：GET 进 query，有 body 的 POST 追加进 form。
- 登录/登出端点显式关闭：`/login/status`、`/login/qr/*`、`/login/cellphone`、`/login`、`/logout`。
- Android 侧由 `SessionInterceptor` 按 path 规则自动注入。

### 1.4 Cookie 会话

- 请求侧：发起请求前把当前会话 cookie（仅 `MUSIC_U` 且确保 `os=pc`）作为 `cookie` 参数下发。
- 响应侧（两条来源都要）：
  1. body 顶层 `cookie` 或 `data.cookie`（`session.ts`);
  2. 响应头 `Set-Cookie`（`api.rs collect_set_cookie`：截 `;` 前部分、剔除空项）;
  合并规则：`extractCookie`（剔除 Path/Domain/Expires/HttpOnly/Secure/SameSite/Max-Age）
  新值覆盖旧键；**合并后不含 `MUSIC_U` 不写入**（防误抹登录态）。
- QR 辅助接口（`/login/qr/*`）请求时不带 cookie、不保存中间响应 cookie。

### 1.5 错误分层与重试

- 错误类型在 Repository 翻译成：业务错误（code≠200）/ 网络错误（IO）/ 解析错误（序列化）/ 空结果（成功+空列表）。
- 只针对**安全的读请求（GET）**做有限重试（≤2 次，200ms/400ms 退避），且仅对
  连接失败 / 5xx / 网关类响应。业务 `code` 失败**不重试**（防重复副作用，读请求场景多为幂等）。
- 协程取消必须向外传播（`rethrow CancellationException`），不并入普通失败。

### 1.6 隔离与日志

- **Cookie/密码/随机 IP 不得发送给封面图片或音频 CDN**：
  图片/媒体加载走独立的 Coil/OkHttp 实例，不挂会话拦截器。
- **日志不得输出 Cookie、密码或包含敏感参数的完整 URL**：调试默认不启用请求日志；
  如需抓包用代理工具，不依赖应用日志。

## 2. 端点清单（本阶段接入）

> 「表达式」为 `client.ts` 的实际参数；HTTP 均 GET 除非注明。参数类型 `SongId` 支持数字/字符串。

### 2.1 `/personalized`（推荐歌单）
- `GET /personalized?limit={n}`；默认 limit 10。
- 响应：`{ code, result: [ playlist ] }`（playlist 元素见 3.2）。
- 业务失败：`code != 200`。空：`result` 缺失/空 → 成功 + 空列表。
- TTL（原项目）：30min。

### 2.2 `/personalized/newsong`（推荐新歌）
- `GET /personalized/newsong?limit={n}`；默认 limit 12。
- 响应：`{ code, result: [ song ] }`（song 元素见 3.1）。
- TTL：30min。

### 2.3 `/cloudsearch`（搜索歌曲）
- `GET /cloudsearch?keywords={k}&limit={n}&offset={off}`；默认 limit 30、offset 0。
- 响应：`{ code, result: { songs: [song], songCount?, hasMore? } }`。
- TTL：未配置（0，不缓存）。

### 2.4 `/playlist/detail`（歌单详情）
- `GET /playlist/detail?id={id}`。
- 响应：`{ code, playlist: { id, name, coverImgUrl|picUrl, description?,
  trackCount, creator:{nickname}, tracks: [song]（可能为部分/空）, trackIds: [{ id, at?, addTime? }] } }`。
- 「完整曲单」以 `trackIds` 为序；`/song/detail` 补查（见 2.5 与 4.2）。
- TTL：30min。

### 2.5 `/song/detail`（批量歌曲详情）
- `GET /song/detail?ids={id1,id2,...}`（逗号分隔，**批量上限 500**/次，见 `details.ts SONG_DETAIL_BATCH_SIZE`）。
- 响应：`{ code, songs: [song] }`。
- TTL：24h。

### 2.6 播放 URL（Loop 5 接入，对齐 `url-resolver.ts` + `client.ts`）
- `GET /song/url/v1?id={id}&level={level}&unblock={true|false}`（主接口）
  - `level ∈ {lossless, exhigh, higher, standard}` 原样透传；`unblock` 是字符串 `"true"/"false"`。
  - 响应：`{ code, data: [ { url, freeTrialInfo? , message?, msg? } ] }`。
  - **试听判定：`freeTrialInfo` 字段存在即试听片段**（`getStream` 的 `isTrial`、`cacheable=false`）。
- `GET /song/url/match?id={id}`（灰色歌曲直连）；响应 data[0].url，个别返回顶层 url。
- `GET /song/url?id={id}&br={320000}`（老接口兜底，按码率）。
- 候选链（`getPlayableUrls` Phase 1-6）：
  1. 快速出声：`standard → higher → 用户偏好`，普通接口非试听首个即中（试听先攒着）；
  2. `unblock=true` 再扫同一序列（同样非试听优先）；
  3. `/song/url/match`；
  4. `/song/url?br=320000`（旧接口）；
  5. 前面攒下的试听候选；
  6. 官方外链模板 `https://music.163.com/song/media/outer/url?id={id}.mp3`。
  - 单级超时 `FAST_TIMEOUT=3500ms`（填充 5000ms），超时/异常视为该级无结果；外层取消上抛。
  - 空字符串 url/null data 视为无结果；`http://*.music.126.net` 统一升级为 https。

### 2.7（对照，非实现）会话端点
`/login/status`、`/user/account`、`/logout` —— phase 1 已定义的 DTO 与拦截器规则见 3.x。

## 3. 领域模型与字段映射

### 3.1 Song（领域模型对齐 `types/music.ts`）
| 领域字段 | 映射（多态收窄，`normalize.ts` 顺序） | 类型 |
|---|---|---|
| `id` | `song.id`（数字或字符串，**统一转 String**） | String |
| `name` | `song.name` | String |
| `artists` | `song.ar`，元素 `{id, name}`；缺省取 `song.artists` | List<Artist> |
| `album` | `song.al`，元素 `{id, name, picUrl}`；缺省取 `song.album` | Album? |
| `durationMs` | `song.dt`；缺省 `song.duration`（统一毫秒） | Long |
| `coverUrl` | `al.picUrl` → `song.coverImgUrl` → `song.picUrl` | String |

其中 `song` 可能来自三层：`root.song` → `root.resourceExtInfo.songData`
（→ `ext.song`）→ `root` 自身（收窄：id 缺失丢弃）。

### 3.2 Playlist（领域模型对齐 `types/music.ts`）
| 字段 | 映射 | 类型 |
|---|---|---|
| `id` | `playlist.id` → String | String |
| `name` | `playlist.name` | String |
| `coverUrl` | `playlist.coverImgUrl` → `playlist.picUrl` | String |
| `trackCount` | `playlist.trackCount` | Int |
| `creatorName` | `playlist.creator.nickname`（缺省空串） | String |

### 3.3 Artist / Album
- Artist：`{ id, name, imageUrl }`（imageUrl 后续阶段取 `picUrl/cover/avatar/img1v1Url` 的优先级链）。
- Album：`{ id, name, coverUrl }`。

## 4. 歌单「按 trackIds 补全」协议

（`details.ts loadPlaylistDetail` 为准）
1. `trackIds[i].id` 为序基准；补查结果**按此顺序重建**曲目序列。
2. 分批：每批 `song/detail` **50** 个（`loadSongsByIds` 批量上限 500，单批 50 便于边载边展示）。
3. 兜底占位：某 id 查不到时用 `{ id, name: "歌曲 {id}", ar: [], al: {}, dt: 0 }` 占位，
   保持序号完整（阶段 3 展示后再接入播放过滤）。
4. **部分结果不得当作完整歌单**：展示进度必须联通 `已加载/总量`。
5. 并发去重：ids 去重后再查，结果回填去重后仍按序。

## 5. 错误模型（Repository 统一出口）

```
sealed interface NeteaseResult<out T>
  Success(data)          // 业务成功；空列表也是 Success，与错误区分
  ApiError(code, msg)    // body.code != 200（或登录态异返回码）
  NetworkError(cause)    // IOException / 超时（非业务失败）
  ParseError(cause, msgs) // 响应结构解析失败（序列化异常）
协程取消 → rethrow，不落入上述任何分支
```

## 6. 本阶段不实现

- 持久化响应缓存（原项目有缓存 TTL——留作后续阶段；本阶段只记录其取值）。
- URL 持久缓存与预取（原项目 dbCache / preload，Loop 5 明确暂缓）。
- 登录 UI 与账号写操作。