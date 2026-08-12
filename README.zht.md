<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen?style=for-the-badge" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Vue-3.4-blue?style=for-the-badge" alt="Vue" />
  <img src="https://img.shields.io/badge/Java-1.8-orange?style=for-the-badge" alt="Java" />
</p>

<h1 align="center">📄 畢業論文自動排版系統</h1>
<p align="center">基於規則配置的一站式論文排版工具，讓排版從幾個小時變成幾分鐘。</p>

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

## ✨ 項目簡介

**畢業論文自動排版系統** 是一個基於規則配置驅動的學術文檔排版工具，通過可視化規則配置 + 自動排版引擎，快速生成符合學校規範的論文。同時內建三線表、ER 圖、系統架構圖、流程 DSL 等實用圖表工具。

核心思想：**用戶配置規則 → 引擎自動排版 → 一鍵下載規範文檔**，無需手動調整字體、字號、行距、頁眉頁腳。

---

## 🎯 功能特性

### 📝 論文智能排版
- **規則配置驅動**：頁面設置（紙張/頁邊距/頁眉頁腳/頁碼）、標題格式（一/二/三級）、正文格式（行距/縮排/對齊）、圖表編號規則，全部可視化配置
- **自動排版引擎**：識別章節結構、統一字體字號、自動生成目錄、摘要/英文摘要獨立分頁、參考文獻格式化
- **圖表題注**：圖片/表格自動編號（`圖3-1`、`表3-1`），可開關控制
- **編號統一化**：自動識別並統一章節/列表編號風格

### 🔐 帳戶安全（v1.0.1 新增）
- **雙重驗證碼**：註冊/登入需通過圖形驗證碼，註冊還須信箱驗證碼（QQ 信箱 SMTP 發送，5 分鐘有效，60 秒限流）
- **BCrypt 密碼加密**：密碼加鹽雜湊儲存，不存明文
- **登入限流**：連續 5 次失敗鎖定 10 分鐘，防暴力破解
- **忘記密碼**：信箱驗證碼重設密碼，重設後舊 Token 全部失效
- **XSS 防護**：全域過濾器對 JSON 請求體轉義，SQL 注入由 MyBatis-Plus 參數化查詢天然防護

### 📊 三線表生成
- 一鍵生成規範三線表 Word 文檔（頂線/底線 1.5pt、欄目線 0.75pt、無豎線）
- 支持從 SQL `CREATE TABLE` 語句自動解析生成
- 即時預覽、多格式匯出

### 🧬 ER 圖生成 (Chen 記法)
- 輸入實體/屬性/關係，自動生成標準 Chen 記法 ER 圖
- 實體矩形、屬性橢圓（主鍵下劃線）、關係菱形、基數嵌入連線
- 後端力導向佈局 + AntV X6 渲染，支持 SVG/PNG 匯出、佈局保存

### 🏗 系統架構圖生成
- **結構化分層配置**：自訂層（客戶端/業務層/服務應用層/數據層...），每層自由添加組件
- 規則引擎自動識別組件類型（Vue/SpringBoot/MySQL/Redis/Nginx...）
- 生成標準分層架構圖（層容器 + 組件卡片 + 虛線分隔），SVG/PNG 匯出

### 🔀 系統設計圖
- **流程 DSL 解析器**：用 `if(條件)/else` + 縮排書寫流程邏輯，自動生成分支流程圖
- 泳道圖：按角色自動分泳道
- Dagre 自動佈局

---

## 🛠 技術棧

| 層 | 技術 |
| --- | --- |
| 後端 | Spring Boot 2.7 · MyBatis-Plus · MySQL 8 · Apache POI · JWT |
| 前端 | Vue 3 · Element Plus · AntV X6 · @antv/layout · html2canvas · Vite |
| 建置 | Maven · npm · JDK 1.8 |

---

## 🚀 快速開始

### 環境要求

> [!IMPORTANT]
> 需要 JDK 1.8+、Maven 3.9+、MySQL 8.0+、Node.js 18+

### 1. 初始化數據庫

創建數據庫 `thesis_format`，系統啟動時自動執行建表腳本。

```bash
mysql -u root -p -e "CREATE DATABASE thesis_format DEFAULT CHARACTER SET utf8mb4;"
```

### 2. 啟動後端

```bash
cd backend
set DB_PASSWORD=你的數據庫密碼
set MAIL_PASSWORD=你的信箱SMTP授權碼   # 可選, 用於信箱驗證碼發送
mvn package -DskipTests
java -jar target/thesis-format.jar
```

> 服務運行於 `http://localhost:8080`
> 默認發件信箱為 `2651896126@qq.com`（SMTP 配置見 `application.yml` 的 `spring.mail`）

### 3. 啟動前端

```bash
cd frontend
npm install
npm run dev
```

> 訪問 `http://localhost:5173`

---

## 📁 項目結構

```
Graduated/
├── backend/                    # Spring Boot 後端
│   └── src/main/java/com/graduate/thesis/
│       ├── controller/         # 接口: 用戶/模板/論文/ER圖/三線表/系統圖
│       ├── service/            # 業務 + 排版引擎 + 圖表生成器
│       ├── engine/             # 排版引擎(結構識別/格式化)
│       └── dto/                # 請求/響應模型
└── frontend/                   # Vue 3 前端
    └── src/
        ├── views/              # 頁面(首頁/登入/模板/ER圖/三線表/系統設計)
        ├── api/                # 接口封裝
        └── router/             # 路由
```

---

## 🔒 配置說明

| 配置項 | 說明 |
| --- | --- |
| `DB_PASSWORD` | 數據庫密碼，通過環境變數配置 |
| `MAIL_PASSWORD` | 信箱 SMTP 授權碼（用於發送信箱驗證碼） |
| `thesis.jwt.secret` | JWT 密鑰（倉庫已忽略 `application.yml`，需自行創建） |
| `thesis.storage.dir` | 文件存儲目錄，默認 `backend/data/storage` |

---

## 📖 使用流程

```
1. 註冊/登入
      ↓
2. 創建格式模板(頁面/標題/正文/圖表規則)
      ↓
3. 上傳論文文檔
      ↓
4. 選擇模板自動排版
      ↓
5. 下載規範論文
```

---

## 🤝 貢獻

歡迎提交 Issue 和 Pull Request。如果是基於本項目開發的衍生項目，請在 README 中註明。

---

## 📄 License

本項目僅供學習交流使用，基於 [MIT License](LICENSE)。
