<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen?style=for-the-badge" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Vue-3.4-blue?style=for-the-badge" alt="Vue" />
  <img src="https://img.shields.io/badge/Java-1.8-orange?style=for-the-badge" alt="Java" />
</p>

<h1 align="center">📄 毕业论文自动排版系统</h1>
<p align="center">基于规则配置的一站式论文排版工具，让排版从几个小时变成几分钟。</p>

<p align="center">
  <a href="README.md">简体中文</a> |
  <a href="README.zht.md">繁體中文</a> |
  <a href="README.en.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479a1?style=flat-square" alt="MySQL" />
  <img src="https://img.shields.io/badge/AntV%20X6-3.x-3B6BFF?style=flat-square" alt="AntV X6" />
  <img src="https://img.shields.io/badge/build-v1.0.1-3B6BFF?style=flat-square" alt="Version" />
</p>

---

## ✨ 项目简介

**毕业论文自动排版系统** 是一个基于规则配置驱动的学术文档排版工具，通过可视化规则配置 + 自动排版引擎，快速生成符合学校规范的论文。同时内置三线表、ER 图、系统架构图、流程 DSL 等实用图表工具。

核心思想：**用户配置规则 → 引擎自动排版 → 一键下载规范文档**，无需手动调整字体、字号、行距、页眉页脚。

---

## 🎯 功能特性

### 📝 论文智能排版
- **规则配置驱动**：页面设置（纸张/页边距/页眉页脚/页码）、标题格式（一/二/三级）、正文格式（行距/缩进/对齐）、图表编号规则，全部可视化配置
- **自动排版引擎**：识别章节结构、统一字体字号、自动生成目录、摘要/英文摘要独立分页、参考文献格式化
- **图表题注**：图片/表格自动编号（`图3-1`、`表3-1`），可开关控制
- **编号统一化**：自动识别并统一章节/列表编号风格

### 🔐 账户安全（v1.0.1 新增）
- **双重验证码**：注册/登录需通过图形验证码，注册还须邮箱验证码（QQ 邮箱 SMTP 发送，5 分钟有效，60 秒限流）
- **BCrypt 密码加密**：密码加盐哈希存储，不存明文
- **登录限流**：连续 5 次失败锁定 10 分钟，防暴力破解
- **忘记密码**：邮箱验证码重置密码，重置后旧 Token 全部失效
- **XSS 防护**：全局过滤器对 JSON 请求体转义，SQL 注入由 MyBatis-Plus 参数化查询天然防护

### 📊 三线表生成
- 一键生成规范三线表 Word 文档（顶线/底线 1.5pt、栏目线 0.75pt、无竖线）
- 支持从 SQL `CREATE TABLE` 语句自动解析生成
- 实时预览、多格式导出

### 🧬 ER 图生成 (Chen 记法)
- 输入实体/属性/关系，自动生成标准 Chen 记法 ER 图
- 实体矩形、属性椭圆（主键下划线）、关系菱形、基数嵌入连线
- 后端力导向布局 + AntV X6 渲染，支持 SVG/PNG 导出、布局保存

### 🏗 系统架构图生成
- **结构化分层配置**：自定义层（客户端/业务层/服务应用层/数据层...），每层自由添加组件
- 规则引擎自动识别组件类型（Vue/SpringBoot/MySQL/Redis/Nginx...）
- 生成标准分层架构图（层容器 + 组件卡片 + 虚线分隔），SVG/PNG 导出

### 🔀 系统设计图
- **流程 DSL 解析器**：用 `if(条件)/else` + 缩进书写流程逻辑，自动生成分支流程图
- 泳道图：按角色自动分泳道
- Dagre 自动布局

---

## 🛠 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7 · MyBatis-Plus · MySQL 8 · Apache POI · JWT |
| 前端 | Vue 3 · Element Plus · AntV X6 · @antv/layout · html2canvas · Vite |
| 构建 | Maven · npm · JDK 1.8 |

---

## 🚀 快速开始

### 环境要求

> [!IMPORTANT]
> 需要 JDK 1.8+、Maven 3.9+、MySQL 8.0+、Node.js 18+

### 1. 初始化数据库

创建数据库 `thesis_format`，系统启动时自动执行建表脚本。

```bash
mysql -u root -p -e "CREATE DATABASE thesis_format DEFAULT CHARACTER SET utf8mb4;"
```

### 2. 启动后端

```bash
cd backend
set DB_PASSWORD=你的数据库密码
set MAIL_PASSWORD=你的邮箱SMTP授权码   # 可选, 用于邮箱验证码发送
mvn package -DskipTests
java -jar target/thesis-format.jar
```

> 服务运行在 `http://localhost:8080`
> 默认发件邮箱为 `2651896126@qq.com`（SMTP 配置见 `application.yml` 的 `spring.mail`）

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

> 访问 `http://localhost:5173`

---

## 📁 项目结构

```
Graduated/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/graduate/thesis/
│       ├── controller/         # 接口: 用户/模板/论文/ER图/三线表/系统图
│       ├── service/            # 业务 + 排版引擎 + 图表生成器
│       ├── engine/             # 排版引擎(结构识别/格式化)
│       └── dto/                # 请求/响应模型
└── frontend/                   # Vue 3 前端
    └── src/
        ├── views/              # 页面(首页/登录/模板/ER图/三线表/系统设计)
        ├── api/                # 接口封装
        └── router/             # 路由
```

---

## 🔒 配置说明

| 配置项 | 说明 |
| --- | --- |
| `DB_PASSWORD` | 数据库密码，通过环境变量配置 |
| `MAIL_PASSWORD` | 邮箱 SMTP 授权码（用于发送邮箱验证码） |
| `thesis.jwt.secret` | JWT 密钥（仓库已忽略 `application.yml`，需自行创建） |
| `thesis.storage.dir` | 文件存储目录，默认 `backend/data/storage` |

---

## 📖 使用流程

```
1. 注册/登录
      ↓
2. 创建格式模板(页面/标题/正文/图表规则)
      ↓
3. 上传论文文档
      ↓
4. 选择模板自动排版
      ↓
5. 下载规范论文
```

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request。如果是基于本项目开发的衍生项目，请在 README 中注明。

---

## 📄 License

本项目仅供学习交流使用，基于 [MIT License](LICENSE)。

---

## 🛠 本地开发

```bash
# 1. 启动后端(需先配置环境变量: DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET/THESIS_ADMIN_PASSWORD/MAIL_PASSWORD)
#    IDEA 运行 ThesisApplication, 等控制台出现 "Started ThesisApplication" 即就绪
# 2. 启动前端
cd frontend && npm install && npm run dev   # http://localhost:5173
```

- 日志同时输出到控制台与 `backend/logs/thesis-format.log`(按天滚动, 保留 7 天), 每行携带 `traceId`;
  前端收到 "系统繁忙 (traceId: xxxxx)" 时, 可直接在后端日志里检索该 traceId 定位堆栈。
- 后端未启动/正在重启时, 前端请求会返回明确的 503 提示, 页面右下角会显示"后端服务未连接"状态。

### 已知限制

- JWT 注销/改密的 token 撤销记录保存在内存中, 后端重启后旧 token 仍有效至自然过期(生产部署需落库)。
- vite 开发服务器在本机只监听 IPv6 `[::1]`, 浏览器通过 `localhost` 访问不受影响; 若需局域网访问请配置 `server.host`。
