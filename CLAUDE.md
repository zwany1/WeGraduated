# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**毕业论文自动排版系统**（v1.0.0）：基于规则配置驱动的学术文档排版工具。核心流程：用户配置格式规则模板 → 上传 docx → 排版引擎按规则自动排版 → 下载规范文档。另内置三线表、ER 图、系统架构图、流程 DSL 四个图表生成工具。

前后端分离：Spring Boot 2.7 后端（`backend/`，JDK 1.8，Maven）+ Vue 3 前端（`frontend/`，Vite 5）。

## 常用命令

```bash
# 后端构建与启动（application.yml 被 gitignore，需先自行创建）
cd backend
mvn package -DskipTests
java -jar target/thesis-format.jar          # 端口 8080

# 前端开发与构建
cd frontend
npm install
npm run dev                                 # 端口 5173，/api 代理到 8080
npm run build

# 数据库：MySQL 8，库名 thesis_format（utf8mb4），启动时自动执行建表脚本
```

- 配置：`DB_PASSWORD` 环境变量；`thesis.jwt.secret`（JWT 密钥）；`thesis.storage.dir` 文件存储目录（默认 `backend/data/storage`）。
- **仓库没有任何测试**（backend 无 test 源码，frontend 无测试脚本）。修改后需手动验证。
- `backend/src/main/resources/` 整体被 .gitignore 忽略，本地启动需自行准备 `application.yml`（参考 README 配置项）。

## 后端架构

包结构：`com.graduate.thesis` — `controller` / `service` / `engine` / `dto` / `entity` / `mapper` / `common` / `config`。

- **统一响应与鉴权**：所有接口返回 `common/Result<T>`；`UserContext` 持有当前登录用户（由 `config/LoginInterceptor` 注入，基于 JWT）；业务异常抛 `common/BusinessException`，由 `GlobalExceptionHandler` 统一转 HTTP 响应。
- **持久层**：MyBatis-Plus 实体（`entity/`，表前缀 `t_`，如 `t_format_rule`、`t_format_task`）；Mapper 均为 `BaseMapper` 接口，无自定义 XML。

### 排版引擎（`engine/`）— 核心链路

`PaperService.startFormat` → 创建 `FormatTask` → `@Async runFormat`（异步执行）→ `FormatEngine.format(source, ruleSet, progress)`。

`FormatEngine` 固定管线（`FormatEngine.java:42`），各环节独立可替换：

1. `StructureDetector.detect(doc, ruleSet)` → 将文档段落分类为 `DocItem`（章节/正文/图表题注/空段等）
2. `PageFormatter`（页面设置）→ `CoverFormatter`（封面）→ `AbstractFormatter`（摘要独立分页）→ `SectionFormatter`（分节）
3. `HeadingFormatter` + `TextFormatter` + `ParagraphFormatter`（标题/正文格式）
4. `CaptionFormatter`（图表题注编号 `图3-1`/`表3-1`）→ `TocFormatter`（目录，可开关）
5. `HeaderFooterFormatter`（页眉页脚）

- **规则模型**：`engine/model/RuleSet` 由模板 + `FormatRule` 列表构建（`RuleSet.from`）。`FormatRule.ruleType`：`heading1/heading2/heading3/body/figure/table`。标题识别用正则（`RuleSet.DEFAULT_HEADING1-3`），编译时自动补 `.*` 以支持前缀式写法。
- **任务状态机**：`FormatTask` 常量 `STATUS_PENDING → PROCESSING → SUCCESS/FAILED`，进度 0-100 由进度回调更新；前端轮询 `/paper/task/{id}`。
- 文件经 `StorageService` 存于 `backend/data/storage`（相对路径入库）。

### 图表生成工具（`service/`，独立子模块）

- **三线表**：`SqlTableParser` 解析 MySQL `CREATE TABLE`（列名/类型/可空/默认值/注释）→ POI 生成规范三线表 Word（顶/底线 1.5pt、栏目线 0.75pt、无竖线）。
- **ER 图（Chen 记法）**：`ERGraphService` + `ErGraph` 模型 + `ERLayoutEngine`（静态分层布局：实体聚类、属性椭圆避让基数标注、减少交叉）→ `ERDiagramRenderer` 输出，前端 AntV X6 渲染。
- **系统架构图**：`ArchitectureRuleEngine` 按用户分层配置自动推断组件形状（关键词匹配：mysql→database、redis→cache、vue→web 等，`shapeOf`）并全连生成连线。
- **流程图**：`FlowParser` 解析缩进 DSL（`if(条件)/else` + 缩进）→ 递归 `parseBlock` 生成分支图，自动加开始/结束节点。

## 前端架构

- **页面路由**（`src/router/index.js`）：`/` 首页、`/login`、`/templates`、`/template/:id`（模板配置）、`/tasks`（排版任务）、`/table3`、`/er`、`/system-design`。`meta.requiresAuth` 由 beforeEach 守卫（localStorage `token`）。
- **API 封装**：`src/api/*.js` 按模块（user/template/paper/table3/er/diagram），axios 统一请求后端 `/api` 前缀（Vite 代理到 8080）。
- **图表工具类**：`src/utils/erDot.js`、`erPostProcess.js`、`erValidate.js` 为 ER 图前端后处理（布局修正/校验）。
- 组件：Element Plus + AntV X6（图表画布）+ @antv/layout（Dagre 布局）+ html2canvas（导出 PNG）。

## 约定与注意事项

- 后端 Java 代码遵循 Allman 风格；前端 Vue 遵循 Prettier 风格（无现成 Prettier 配置，保持一致即可）。
- 排版引擎改动影响面大（`FormatEngine` 固定管线顺序），新增格式化环节时遵循现有 `AbstractFormatter` 策略模式。
- 图表布局/解析类（`ERLayoutEngine`、`SqlTableParser`、`FlowParser`）为静态工具类，无 Spring 依赖，可独立单测。
- 用户数据与文件存储（`backend/data/`）不入库管理外的敏感内容，勿将本地 `application.yml` 提交。
- 检测到本机存在 `~/.codex/config.toml` 与 `~/.gemini/settings.json`；如需导入相关配置（MCP 服务器、技能、指令等），可在终端回复 `/import` 扫描后按提示导入。
