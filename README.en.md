<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen?style=for-the-badge" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Vue-3.4-blue?style=for-the-badge" alt="Vue" />
  <img src="https://img.shields.io/badge/Java-1.8-orange?style=for-the-badge" alt="Java" />
</p>

<h1 align="center">📄 Thesis Format System</h1>
<p align="center">A rule-based one-stop thesis formatting tool that turns hours of layout work into minutes.</p>

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

## ✨ Introduction

**Thesis Format System** is a rule-configuration-driven academic document formatting tool. It combines visual rule configuration with an automatic formatting engine to quickly produce school-standard theses. It also includes practical diagram tools: Three-Line Table, ER Diagram, System Architecture Diagram, and Flow DSL.

Core idea: **configure rules → engine formats automatically → download compliant document**, no manual adjustment of fonts, sizes, line spacing, headers, or footers.

---

## 🎯 Features

### 📝 Smart Thesis Formatting
- **Rule-driven configuration**: page setup (paper/margins/header-footer/page numbers), heading styles (level 1/2/3), body format (line spacing/indent/alignment), figure & table numbering — all visually configurable
- **Automatic formatting engine**: detects chapter structure, unifies fonts/sizes, auto-generates TOC, abstract/English abstract on separate pages, reference formatting
- **Figure & table captions**: auto-numbering (`图3-1`, `表3-1`), toggleable
- **Number unification**: auto-detects and unifies chapter/list numbering styles

### 🔐 Account Security (new in v1.0.1)
- **Dual captcha**: graphic captcha required for login/register; email verification code also required for registration (sent via QQ SMTP, valid 5 min, 60 s rate limit)
- **BCrypt password hashing**: salted hash storage, no plaintext
- **Login rate limiting**: locks account 10 min after 5 consecutive failures
- **Forgot password**: reset via email code; all old tokens invalidated after reset
- **XSS protection**: global filter escapes JSON request bodies; SQL injection prevented natively by MyBatis-Plus parameterized queries

### 📊 Three-Line Table Generator
- One-click standard three-line table Word document (top/bottom 1.5pt, column line 0.75pt, no vertical lines)
- Parse from SQL `CREATE TABLE` statements automatically
- Live preview, multi-format export

### 🧬 ER Diagram (Chen Notation)
- Auto-generate standard Chen-notation ER diagrams from entities/attributes/relationships
- Entity rectangles, attribute ellipses (underlined PK), relationship diamonds, cardinality embedded in lines
- Backend force-directed layout + AntV X6 rendering, SVG/PNG export, layout saving

### 🏗 System Architecture Diagram
- **Structured layered config**: custom layers (client/business/service/data...), freely add components per layer
- Rule engine auto-detects component types (Vue/SpringBoot/MySQL/Redis/Nginx...)
- Standard layered architecture diagram (layer containers + component cards + dashed separators), SVG/PNG export

### 🔀 System Design Diagram
- **Flow DSL parser**: write flow logic with `if(condition)/else` + indentation, auto-generate branch flowcharts
- Swimlane diagram: auto-lane by roles
- Dagre auto layout

---

## 🛠 Tech Stack

| Layer | Tech |
| --- | --- |
| Backend | Spring Boot 2.7 · MyBatis-Plus · MySQL 8 · Apache POI · JWT |
| Frontend | Vue 3 · Element Plus · AntV X6 · @antv/layout · html2canvas · Vite |
| Build | Maven · npm · JDK 1.8 |

---

## 🚀 Quick Start

### Requirements

> [!IMPORTANT]
> JDK 1.8+, Maven 3.9+, MySQL 8.0+, Node.js 18+

### 1. Initialize Database

Create database `thesis_format`; schema auto-executes on startup.

```bash
mysql -u root -p -e "CREATE DATABASE thesis_format DEFAULT CHARACTER SET utf8mb4;"
```

### 2. Start Backend

```bash
cd backend
set DB_PASSWORD=your_db_password
set MAIL_PASSWORD=your_email_smtp_code   # optional, for email verification codes
mvn package -DskipTests
java -jar target/thesis-format.jar
```

> Backend runs at `http://localhost:8080`
> Default sender is `2651896126@qq.com` (see `spring.mail` in `application.yml`)

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

> Visit `http://localhost:5173`

---

## 📁 Project Structure

```
Graduated/
├── backend/                    # Spring Boot backend
│   └── src/main/java/com/graduate/thesis/
│       ├── controller/         # APIs: user/template/paper/ER/table3/diagram
│       ├── service/            # business + format engine + diagram generators
│       ├── engine/             # format engine (structure detect/format)
│       └── dto/                # request/response models
└── frontend/                   # Vue 3 frontend
    └── src/
        ├── views/              # pages (home/login/template/ER/table3/design)
        ├── api/                # API wrappers
        └── router/             # routes
```

---

## 🔒 Configuration

| Key | Description |
| --- | --- |
| `DB_PASSWORD` | Database password via environment variable |
| `MAIL_PASSWORD` | Email SMTP authorization code (for sending verification codes) |
| `thesis.jwt.secret` | JWT secret (repo ignores `application.yml`, create yourself) |
| `thesis.storage.dir` | Storage directory, default `backend/data/storage` |

---

## 📖 Usage Flow

```
1. Register/Login
      ↓
2. Create format template (page/heading/body/figure rules)
      ↓
3. Upload thesis document
      ↓
4. Auto-format with template
      ↓
5. Download compliant thesis
```

---

## 🤝 Contributing

Pull requests and issues are welcome. If you build a derivative project based on this code, please note it in your README.

---

## 📄 License

For learning and communication only, under [MIT License](LICENSE).
