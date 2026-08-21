# 📄 毕业论文自动排版系统 — 全面代码审查报告

> **审查范围**：后端 Spring Boot 2.7（约 180 个 Java 文件）＋ 前端 Vue 3 + Element Plus（约 70 个文件）
> **审查维度**：安全 → 逻辑 → 性能 → 可读性
> **日期**：自动化静态审查
> **说明**：以下结论来自对源码的直接阅读与多路交叉验证（后端安全核心、前端组件、排版引擎三条独立审查流汇总）。所有条目均标注 `文件:行号` 与修改建议。

---

## 🎯 结论速览

| 等级 | 数量 | 代表问题 |
|------|-----|---------|
| 🔴 严重 Critical | 4 | 明文密码持久化、JWT 进 URL、公告 v-html XSS、TOC 字体乱码 |
| 🟠 高 High | 13 | 默认管理员密码、内存 Map DoS、验证码/登录限流可伪造 IP、审计日志泄密、弱密码、并发数据竞争、前端清理/内存泄漏 |
| 🟡 中 Medium | 18 | 全局 zip 防护、临时文件/流泄漏、N+1 查询、备份明文密码、Excel 公式注入、调度解耦、XssFilter 上限绕过、`else if`/括号解析、前端响应式/裸 fetch 等 |
| 🔵 低 Low | ~10 | ObjectMapper 复用、组件过大、权限双模型、硬编码等 |

---

## 🔴 严重（Critical）— 必须优先修复

### C1. 「记住我」把明文密码写进 localStorage（前端）

**文件**：`frontend/src/views/Login.vue:209-210`
```js
localStorage.setItem('remembered_pwd', btoa(encodeURIComponent(form.password)))
```
- **问题**：`btoa` 只是 Base64 **编码**，`atob` 即可还原明文密码。任何 XSS、浏览器扩展或本机可访问者都能拿到所有保存的密码。
- **改法**：彻底移除密码持久化。改用后端签发的 `httpOnly + Secure` 长令牌做「记住我」，前端只存用户名。

### C2. JWT 通过 URL query 传递（SSE 订阅，前后端均有）

- 前端 `frontend/src/views/FormatTask.vue:241`：
  ```js
  const es = new EventSource(`/api/paper/task/${taskId}/progress?token=${token}`)
  ```
- 后端 `backend/src/main/java/com/graduate/thesis/controller/PaperController.java:77-99` 从 `?token=` 手动鉴权

- **问题**：Token 进 URL 会被 Web 服务器/代理/CDN 访问日志、浏览器历史、Referer 一并记录，直接泄露长期有效的 JWT（`expire-hours: 72`）。
- **改法**：SSE 首次连接时后端签发一次性短期会话凭据；或改用 WebSocket 走 `Authorization` 头；或设置短生命周期的 `SameSite=Strict; Secure; HttpOnly` cookie。

### C3. 公告内容用 `v-html` 渲染（存储型 XSS 风险点）（前端）

**文件**：`frontend/src/views/Home.vue:339`
```html
<div class="notice-detail-content" v-html="currentNotice.content"></div>
```
- **问题**：公告 `content` 是后台可写的富文本。一旦某字段进入后端 `XssFilter` 的 `NO_ESCAPE_KEYS`，或未来放开该字段，`v-html` 会直接执行注入的 `<img onerror=...>` / `<script>`，窃取所有访客 token。
- **改法**：改用文本插值 `{{ currentNotice.content }}`；或在渲染前用 DOMPurify 消毒；并在后端存储前做白名单过滤。

### C4. 排版引擎 TOC 字体名出现乱码（后端）

**文件**：`backend/src/main/java/com/graduate/thesis/engine/formatter/TocFormatter.java:100-102`
```java
addTocStyle(cts, "toc1", 28, "����");   // 预期的中文字体名已被编码损坏
```
- **问题**：源码字面量被编码损坏成 `����`，会原样写进 Word 的 `eastAsia` 字体名，导致生成的目录（TOC）中文回退到系统默认字体。
- **改法**：将三处 `"����"` 改回正确的中文字体名（如 `"黑体"` / `"宋体"`），并确保文件以 UTF-8 保存；同时排查仓库其余位置是否有编码损坏。

---

## 🟠 高（High）— 尽快修复

### H1. 默认管理员密码 `admin123`（后端，默认凭据漏洞）

**文件**：`backend/src/main/java/com/graduate/thesis/config/DbMigrationRunner.java:52-53, 301-317`
```java
@Value("${thesis.admin.username:admin}") String adminUsername,
@Value("${thesis.admin.password:admin123}") String adminPassword
```
- **问题**：若部署时未设置 `THESIS_ADMIN_PASSWORD`，系统以**人人皆知的管理员/默认密码**上线（`ensureAdminAccount()` 幂等创建）。且只在「账号不存在」时创建，事后改 env 不会旋转已存在的密码。
- **改法**：去掉默认值——`thesis.admin.password` 未配置时启动直接失败；治理启动期双套 DDL 引导机制（`spring.sql.init.mode: always` + 迁移 Runner）。

### H2. 内存态登录/限流 Map 无清理，可被 DoS（后端）

- `backend/.../service/UserService.java:67-73`：`failCount / lockUntil / ipFailCount / ipLockUntil` 四个 `ConcurrentHashMap` 只增不清理，攻击者用大量随机用户名/IP 可撑爆堆内存。
- `backend/.../service/EmailCodeService.java:34-36`：`store / ipWindow` 仅在超限/校验时清理，过期条目累积。
- `backend/.../util/JwtUtil.java:25`：`revokedTokens` 存全部 JWT，仅惰性清理。

- **改法**：改用 `Caffeine`/`Guava` 缓存（`expireAfterWrite`），或定时清理 + 容量上限。

### H3. 邮箱/图形验证码限流可被伪造 IP 绕过（后端）

`EmailCodeService.clientIp()` / `UserController.clientIp()` / `LogService.getClientIp()` 都**直接信任 `X-Forwarded-For` / `X-Real-IP`**。若 Nginx 未强制覆盖这些头，攻击者可伪造 IP 头绕过「单 IP 每小时 10 封」的邮箱轰炸限制和登录 IP 锁定。
- **改法**：在反向代理层**覆写而非透传**这些头（`proxy_set_header X-Real-IP $remote_addr;`），后端只信任已覆写的头。

### H4. 重置密码路径无图形验证码 + 6 位邮箱码无双因子（后端）

`UserController.reset-password`（在 WebConfig 白名单内）：只校验 6 位邮箱验证码，**不校验图形验证码，也没有尝试次数限制**。6 位码仅 100 万种组合，结合可绕过的 IP 限流（H3），存在账号接管风险。
- **改法**：重置密码也要求图形验证码；对 `verify()` 增加每邮箱/每 IP 的尝试上限。

### H5. 管理后台「前端路由守卫」仅是 UX（前端，依赖后端兜底）

`frontend/src/router/index.js:138-148` 只按「菜单里有没有该路由」判断，篡改 `localStorage.menus` 即可进入任意管理页。
- **核查结论**：后端**所有** `/admin/**` 接口均带 `@RequiresPerms`，服务端权限校验完备（`AdminController / SystemController / MenuController / BackupController` 等），当前**不构成越权**。但这是「安全靠后端、前端不设防」的脆弱模式，任何后端遗漏即变提权。建议在路由守卫加注释注明「仅 UX，安全由服务端保证」。

### H6. 管理员重置密码等操作把明文密码写入审计日志（后端）

`OperLogAspect.buildParams()`（`backend/.../config/OperLogAspect.java:48-60`）会把 `@OperLog` 方法的**全部入参序列化成 JSON 存库**。`AdminController.resetUserPassword` 带 `@OperLog` 且入参 `Map{password:...}` → **明文密码被写进操作日志**。
- **改法**：`buildParams` 中屏蔽 `password` 等敏感字段（复用 XssFilter 的 `NO_ESCAPE_KEYS` 思路），或用独立 DTO + 注解排除。

### H7. 密码强度校验过弱（后端）

`UserService.checkPasswordStrength()` 只要求「含字母 + 数字」，`"a1"` 即可通过，无最小长度。
- **改法**：至少 `长度≥8` 且字母 + 数字；最好再禁用常见弱密码/连续字符。

### H8. ERLayoutEngine 静态可变状态（后端，并发数据竞争）

**文件**：`backend/.../service/ERLayoutEngine.java:34-35, 41-42`
```java
private static List<ErGraph.Entity> allEntities = new ArrayList<>();
private static List<ErGraph.Relation> allRelations = new ArrayList<>();
allEntities = g.entities;   // 每次 layout() 重新赋值
```
- **问题**：并发生成 ER 图时，请求 A 的实体列表会被请求 B 覆盖，导致布局错乱、跨请求数据串扰，甚至 `ConcurrentModificationException`。
- **改法**：把 `allEntities/allRelations` 改为方法参数逐层传递，或改用实例级/每请求对象，或同步 `layout()`。

### H9. 各类图服务单例上的 `seq` 计数器线程不安全（后端）

`DiagramGenerator.java:44`、`FlowParser.java:24`、`ActivityDiagramRuleEngine.java:31`、`SequenceRuleEngine.java:28` 都是 `@Service` 单例上的 `private int seq`。并发请求共享并互踩该计数器，导致节点/边 ID 重复、图渲染错乱。
- **改法**：`seq` 改为方法内局部变量，或 `AtomicInteger`（不重置），或 service 改 prototype 作用域。

### H10. 其余 `v-html`/innerHTML 显式化说明（前端）

在 C3 之外，`Home.vue:278(s.desc)`、`Features/Guide/Home` 的 `v-html="icon"` 均为开发者硬编码 SVG，安全；`SystemDesign.vue:1499 inner.innerHTML` 已用 `esc()` 转义，属**受控**的显式化。仅提示：这类写入点要一直保持转义，勿引入未转义用户数据。

### H11. 登录/重置流程存在账户枚举（后端）

`UserService.login` 对「用户名不存在」与「密码错误」返回不同消息，`resetPassword` 直接提示「该邮箱未注册」——存在**账户枚举**。
- **改法**：登录失败统一提示「用户名/邮箱或密码错误」；重置密码流程不回显「是否已注册」。

### H12. 首页组件内存泄漏（前端）

`frontend/src/views/Home.vue`：`IntersectionObserver`（L492）与多个 `window.addEventListener('scroll'/'mousemove')`（L506-541）均**未在 `onBeforeUnmount` 中 disconnect / removeEventListener**。反复进出首页会累积监听器与旧组件作用域引用。
- **改法**：保存 observer/句柄引用，卸载时统一清理。

### H13. 登出/注销后 localStorage 清理不完整（前端）

`Home.vue:443-457` 登出只清 `token/username/avatar/role`，**漏掉 `userId/roles/perms/menus`**；`Profile.vue:140` 账号注销用 `localStorage.clear()` 全清（误伤同源其它数据）。残留的 `roles/perms` 会影响 `hasPerm/isAdmin` 判断。
- **改法**：统一改用 `src/utils/perm.js` 的 `clearAuth()`。

---

## 🟡 中（Medium）— 建议修复

### 后端

| # | 位置 | 问题与改法 |
|---|------|-----------|
| M1 | `FormatEngine.java:50`、`DiffService.java:31` | `ZipSecureFile.setMinInflateRatio(0.001)` 是 **JVM 全局静态**设置，放宽了整进程的 zip 炸弹防护，且非线程安全。改法：尽量用 `StorageService.checkZipBomb` 的 200MB 解压上限兜底，或在解析周边局部设置/还原默认。 |
| M2 | `FormatEngine.java:51`、`DiffService.java:70` | `new XWPFDocument(new FileInputStream(...))`，构造失败时 `FileInputStream` 未关闭（Windows 会锁文件）。改法：`try (FileInputStream fis = ...; XWPFDocument doc = new XWPFDocument(fis))`。 |
| M3 | `FormatEngine.java:43-88` | 排版成功后 `temp` 临时 .docx **从不删除**（成功也不删，失败更不删），长跑会填满临时目录。改法：成功由调用方删除、失败在 finally 删除。 |
| M4 | `FormatEngine.java:102-104` | 错误匹配优先级陷阱：`A||B||C||(D&&zip)`，前三个匹配不要求含 "zip"，仅 `IllegalArgumentException` 受 zip 约束（与缩进意图不符）。改法：加括号统一语义。 |
| M5 | `FormatEngine.java:118-119` | 直接改 `ruleSet.rule("body")` 返回的共享 `FormatRule` 对象并缓存，重试/复用会污染源规则。改法：局部 `boolean bold = ...`，不落回对象。 |
| M6 | `TemplateService.java:242-243, 414` | 模板市场/收藏列表遍历 `map()` 内逐个 `selectCount` → **N+1 查询**。改法：批量 `groupingBy` 统计规则数一次查回。 |
| M7 | `BackupService.java:71,151` | `--password=` 明文密码出现在命令行参数，服务器 `ps`/任务管理器可见。改法：改用 `MYSQL_PWD` 环境变量或 `--defaults-extra-file`。 |
| M8 | `FlowParser.java:127-128` | `indexOf(')')` 取**第一个** `)`，`if(foo(bar))` 被截断成 `foo(bar`。改法：用 `lastIndexOf(')')` 或括号深度匹配。 |
| M9 | `FlowParser.java:122-124,86` | `else if(...)` 不被识别，被当普通节点处理，图拓扑错误。改法：支持 `else if` 作为 else 分支后接新 if。 |
| M10 | `SqlTableParser.java:32-33` | `'[^']*'` 无法解析转义单引号 `''`，`DEFAULT 'it''s'` 解析错。改法：`'(?:[^']|'')*'`。 |
| M11 | `DiffService.java:68-76` | 关闭文档后仍使用返回的 `XWPFParagraph` 读样式，可能取到失效数据。改法：在 try 内完成读取。 |
| M12 | `AdminController.java:311-317` | 用户报表把 `errorMsg` 等写进 Excel 单元格，未做**公式注入**防护（以 `=`、`+`、`-`、`@` 开头会触发）。改法：单元格值前置 `'` 或转义公式起始字符。 |
| M13 | `TaskScheduler.java` + `AsyncConfig.java` | DB 侧「PROCESSING」上限（默认 4，可配到 20）与 `formatExecutor` 线程池（max 8）**解耦**：并发上限配 >8 时，被标记 PROCESSING 但未真正跑的任务会卡住调度吞吐。改法：上限与线程池容量一致或改由信号量统一控制。 |
| M14 | `XssFilter.java:40,45` | 请求体上限用 `getContentLength()`；**分块传输（无 Content-Length）时返回 -1，绕过 30MB 上限**并全量缓冲进内存 → DoS 向量。改法：读取时按字节数硬限流。 |

### 前端

| # | 位置 | 问题与改法 |
|---|------|-----------|
| M15 | `DocxCompare.vue:33-38` | `bodyStyle` 是普通对象（非 `reactive`/`computed`），缩放按钮改 `scale` 不会触发 `:style` 更新（UI 不变化）。改法：改 `computed`。 |
| M16 | `SystemDesign.vue:924-930` | 用裸 `fetch()` 绕过 axios 拦截器（缺 401 跳转/统一错误/超时）。改法：抽到 `api/er.js` 走统一实例。 |
| M17 | `admin/UserManage.vue:376-378` | 重置密码只限最小长度不限最大，超长密码可能导致 DB 列溢出/bcrypt 开销。改法：增加上限（如 64）。 |
| M18 | `directives/perm.js:14-16` | `v-perm` 直接 `removeChild` 元素，破坏 Vue 响应式与可访问性树。改法：改 `el.style.display='none'` 或销毁组件分支。 |

---

## 🔵 低（Low）/ 可读性

- **性能杂项**
  - `CoverConfig.java:37`、`ReferenceConfig.java:47`、`RuleSet.java:52`：每次 parse `new ObjectMapper()`（昂贵，应复用单例）。
  - 前端 `perm.js:hasPerm` 每次 `JSON.parse(localStorage)`（可缓存）。

- **可维护性**
  - `Home.vue` 1700+ 行、`SystemDesign.vue` 2187 行，DOM 操作/动画/鉴权混在一起 → 建议拆 composable/子组件。
  - `StructureDetector` 与 `CaptionFormatter` 大量重复（`isTocEntry`/`isDateLike`/`headingLevel` 等），且 `isTocEntry` 两处实现**不一致**，可能产生不同 TOC 判定 → 提取共享工具类。
  - `AdminService.updateUserRole`（单角色串）与 `assignUserRoles`（角色列表）+ `user.role` 列 + `role/user_role` 表**双套权限模型**并存并互相同步，复杂且易失步 → 收敛为单一 RBAC，`user.role` 仅作兼容只读。
  - `CoverFormatter.java:19-30`：学校名/声明文本硬编码在 Java 里 → 应可配置。
  - `TocFormatter.java:131-138`：用反射改 POI 私有字段，脆弱（改版本即坏）→ 用公开 API。
  - `ErGraph.java`：字段全 `public` 无封装。
  - JWT 放 localStorage 已是普遍缺陷（参见 C1/C2），建议整体迁移到 httpOnly cookie；后端 `LoginSessionService.lastActive` 把所有 JWT 常驻堆内存（含 dump 泄露风险）。

---

## ✅ 建议的修复优先级

1. **先修 4 个 Critical**：C1 明文密码 → C2 token 进 URL → C3 公告 v-html XSS → C4 TOC 乱码。
2. **再处理 High 中最易被利用的三类**：H1 默认管理员密码、H2/H3 限流可绕过/内存 DoS、H8/H9 并发竞争。

---

## 📎 附：相关文件索引

**后端（`backend/src/main/java/com/graduate/thesis/...`）**
`config/DbMigrationRunner.java`、`config/XssFilter.java`、`config/OperLogAspect.java`、`config/TaskScheduler.java`、`config/AsyncConfig.java`、`util/JwtUtil.java`、`service/UserService.java`、`service/EmailCodeService.java`、`service/CaptchaService.java`、`service/LoginSessionService.java`、`service/BackupService.java`、`service/PaperService.java`、`service/TemplateService.java`、`service/ERLayoutEngine.java`、`service/FlowParser.java`、`service/DiagramGenerator.java`、`service/ActivityDiagramRuleEngine.java`、`service/SequenceRuleEngine.java`、`service/SqlTableParser.java`、`service/DiffService.java`、`engine/FormatEngine.java`、`engine/formatter/TocFormatter.java`、`controller/AdminController.java`、`controller/PaperController.java`、`controller/UserController.java`

**前端（`frontend/src/...`）**
`views/Login.vue`、`views/Home.vue`、`views/Profile.vue`、`views/FormatTask.vue`、`views/SystemDesign.vue`、`views/admin/UserManage.vue`、`components/DocxCompare.vue`、`components/FeedbackDialog.vue`、`directives/perm.js`、`utils/perm.js`、`api/index.js`、`router/index.js`
