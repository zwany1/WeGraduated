# e2e · 浏览器端到端冒烟

零依赖的 CDP 驱动（手写 WebSocket，不用 playwright / puppeteer），真点真拖，
用来验证「用户在页面上点了会发生什么」—— 单测证明不了的那部分。

## 为什么需要它

`XssFilter` 把请求体里的 `->` 转义成 `-&gt;`、`parseErSql` 只提交了调用方没提交被调用方，
这两类问题单测全绿、页面全错，只有真开浏览器才暴露。

## 前置

1. **后端** 跑在 `13355`，**前端 vite** 跑在 `5173`
2. **chromium** 带调试端口启动（headless 也行）：
   ```
   chrome.exe --headless=new --remote-debugging-port=9333 --user-data-dir=D:\tmp\cdp-profile about:blank
   ```
   注意：headless 进程会随启动它的 shell 退出而结束，要在同一条命令或后台任务里跑。

## 造登录态（验证码过不去，只能绕过）

登录验证码是 点选/滑动/旋转 三选一随机，自动化一律失败；而 `LoginInterceptor` 除了验 JWT 签名，
还要求 `t_login_session` 里有对应会话行。所以：

```bash
# 1) 用后端同一个密钥自签一个 24h token
JWT_SECRET=graduate-thesis-format-secret-key-2026-please-change-me node e2e/gen-token.mjs > e2e/.token

# 2) 打印可直接执行的会话 SQL，插进库里
JWT_SECRET=<同一个> node e2e/gen-token.mjs --sql
mysql -uroot thesis_format -e "<上一步输出的两行 SQL>"

# 或者直接把 token 塞进环境变量，跳过文件
E2E_TOKEN=<token> node e2e/scenarios/sequence-canvas.mjs
```

`e2e/.token` 已在 `.gitignore` 里，不会误提交。

## 跑场景

```bash
node e2e/scenarios/sequence-canvas.mjs   # 时序图: 生成/表单回填/画布编辑/持久化
node e2e/scenarios/flow-arch.mjs         # 流程图+架构图: 判断菱形/分支标签/分层/错误定位
node e2e/scenarios/class-usecase.mjs     # 类图+用例图: 成员结构/关系名与基数/include 与 extend/错误定位
node e2e/scenarios/lane-activity-er.mjs  # 泳道/活动/ER + 8 种图全量冒烟
node e2e/scenarios/compat-load.mjs       # 存量兼容: 打开老图回填文本 / 无配置留空不阻断 / 表单降级
```

断言失败时进程退出码为 `3`，可以直接当门禁用。截图落在 `e2e/shot-*.png`。

### sequence-canvas.mjs 覆盖什么

| 断言 | 对应验收 |
| --- | --- |
| 文本生成时序图、无行号报错 | 票 01 文本主入口 |
| 精确调整表单与画布一致 | 票 01 表单回填（R4） |
| 配置面板默认折叠 | 票 02 验收 3（R6） |
| 双击节点弹出内联编辑框 | 票 02 画布编辑（R10） |
| 改名后文本视图同步 | 票 02 验收 1 |
| 表单参与者同步 | 票 02 验收 1 附带 |
| 用改后文本重新生成不丢改动 | 票 02 验收 4 |
| 拖动后位置随保存落盘 | 票 02 验收 2 |

### flow-arch.mjs 覆盖什么

| 断言 | 对应验收 |
| --- | --- |
| 流程图文本出图、无报错 | 票 03 验收 1 |
| `合格?` 渲染为菱形（判断） | 票 03 验收 1 |
| 分支标签「是」「否」出现在连线上 | 票 03 验收 1 |
| 架构图三层顺序与组件正确 | 票 03 验收 2 |
| 流程图缺连接符指到第 2 行 | 票 03 错误定位 |
| 架构图缩进不一致指到行 | 票 03 验收 3 |

### class-usecase.mjs 覆盖什么

| 断言 | 对应验收 |
| --- | --- |
| 类解析出属性(标题: String)与方法(提交()) | 票 04 验收 1 |
| 关系名「属于」与基数上屏 | 票 04 验收 1 |
| 方法缺可见性前缀指到第 3 行 | 票 04 验收 3 |
| 参与者与用例都上图 | 票 04 验收 2 |
| `«include»` / `«extend»` 标签 | 票 04 验收 2 |
| 用例图无法识别的行指到行 | 错误定位 |

### lane-activity-er.mjs 覆盖什么

| 断言 | 对应验收 |
| --- | --- |
| 泳道内节点与连线条件上图 | 票 05 验收 1 |
| 活动图判断节点为菱形、分支条件上屏 | 票 05 验收 2 |
| ER 实体/属性/主键上图，基数 1/N 上屏 | 票 05 验收 3 |
| ER 遇 @title 明确报错 | 边界 |
| **8 种图示例文本逐一生成** | 票 05 验收 5（全量回归） |

> 切 tab 后至少等 900ms 再点生成：否则 type 还没切完，生成用的仍是上一个图的文本（踩过）。

### compat-load.mjs 覆盖什么

| 断言 | 对应验收 |
| --- | --- |
| 清空文本后纯表单仍能生成 | 票 06 验收 3（降级通道） |
| 保存的 JSON 带 config、不带 dsl | 票 06 验收 1 前提 |
| 打开列表能列已保存的图 | 新增入口 |
| 有配置无文本的图打开后被回填 | 票 06 验收 1 |
| **手工造老格式 JSON（只有 nodes）打开不失败** | 票 06 验收 2/4 |
| 非法 DSL 报错后表单仍可生成 | 票 06 验收 3 |

## 踩过的坑（改脚本前先看）

- **chromium 调试端点是 keep-alive 的**：按 `Content-Length` 读完就 resolve，等 socket `end` 会永远挂起。
- **`.x6-node` 是 SVG 的 `<g>`，没有 `innerText`**，取文字要用 `textContent`。
- **模板字符串里的正则要写 `\\s`**：写成 `\s` 会退化成匹配字母 `s`，把 `Users` 读成 `U er`。
- **X6 的 `shape: 'text'` 在 fromJSON 里会抛 `The shape should be specified`**，要文字载体请用透明底 `rect`。
- 画布缩放后 `getBoundingClientRect()` 给的是屏幕坐标，可直接喂给 `Input.dispatchMouseEvent`；
  但写回模型的是 **graph 坐标**，两者不要混用。
