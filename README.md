# 毕业论文自动排版系统 (Thesis Format)

基于 **Spring Boot + Vue 3** 的一站式毕业论文/学术文档排版工具，通过可视化规则配置自动完成论文格式排版，并提供三线表生成、ER 图生成、系统架构图生成、流程 DSL 等实用工具。

## ✨ 核心功能

### 1. 论文智能排版
- **规则配置驱动**：页面设置（纸张/页边距/页眉页脚/页码）、标题格式（一级/二级/三级标题字体字号）、正文格式（行距/首行缩进/对齐）、图表编号规则，全部可视化配置
- **自动排版引擎**：识别章节结构、统一字体、自动生成目录、摘要/英文摘要独立分页、参考文献格式化
- **图表题注**：图片/表格自动编号（`图3-1`、`表3-1`），支持开关控制
- **编号统一化**：自动识别并统一章节/列表编号风格
- **三线表**：一键生成规范三线表 Word 文档，支持从 SQL `CREATE TABLE` 语句自动解析生成

### 2. ER 图生成 (Chen 记法)
- 输入实体/属性/关系，自动生成标准 Chen 记法 ER 图
- 实体矩形、属性椭圆（主键下划线）、关系菱形、基数嵌入连线
- 后端力导向布局，前端 AntV X6 渲染
- 支持 SVG/PNG 导出、布局保存

### 3. 系统架构图生成
- **结构化配置驱动**：自定义分层（客户端/业务层/服务应用层/数据层/数据库...），每层自由添加组件
- 规则引擎自动识别组件类型（Vue/SpringBoot/MySQL/Redis/Nginx...）
- 生成标准分层架构图（层容器 + 组件卡片 + 虚线分隔）
- 支持 SVG/PNG 导出（html2canvas）

### 4. 系统设计图
- **流程图 DSL 解析器**：用 `if(条件)/else` + 缩进书写流程逻辑，自动生成分支流程图
- 泳道图：按角色自动分泳道
- Dagre 自动布局

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.7、MyBatis-Plus、MySQL 8、Apache POI、JWT |
| 前端 | Vue 3、Element Plus、AntV X6、@antv/layout、html2canvas、Vite |
| 语言 | Java 8、JavaScript (ESM) |

## 📁 项目结构

```
Graduated/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/
│   │   ├── controller/      # 接口: 用户/模板/论文/ER图/三线表/系统图
│   │   ├── service/         # 业务逻辑 + 排版引擎 + 图表生成器
│   │   ├── engine/          # 排版引擎(结构识别/格式化器)
│   │   └── dto/             # 请求/响应模型
│   └── pom.xml
└── frontend/                # Vue 3 前端
    └── src/
        ├── views/           # 页面(Home/登录/模板/ER图/三线表/系统设计)
        ├── api/             # 接口封装
        └── router/          # 路由
```

## 🚀 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.9+
- MySQL 8.0+
- Node.js 18+

### 1. 数据库
创建数据库 `thesis_format`，启动后系统自动执行建表脚本。

### 2. 后端
```bash
cd backend
set DB_PASSWORD=你的数据库密码
mvn package -DskipTests
java -jar target/thesis-format.jar
```
> 服务运行在 `http://localhost:8080`

### 3. 前端
```bash
cd frontend
npm install
npm run dev
```
> 访问 `http://localhost:5173`

## 🔒 配置说明
- 数据库密码通过环境变量 `DB_PASSWORD` 配置
- JWT 密钥在 `application.yml` 中配置（仓库已忽略该文件，需自行创建）
- 存储目录默认为 `backend/data/storage`

## 📄 License
仅供学习交流使用。
