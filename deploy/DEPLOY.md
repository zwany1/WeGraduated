# Docker 部署指南

基于 Docker Compose 一键部署论文排版系统（后端 + 前端 + MySQL）。

## 一、服务器要求

| 依赖 | 说明 |
|---|---|
| Linux 服务器 | 推荐 Ubuntu 20.04+/CentOS 8+，2 核 4G 起 |
| Docker | 20.10+ |
| Docker Compose | v2（`docker compose` 命令） |
| 域名 | 公网访问需要（可选，IP 直访也可） |

## 二、快速部署

### 1. 上传项目到服务器

```bash
# 本地
scp -r D:\project\Graduated root@你的服务器IP:/opt/thesis
```

### 2. 配置密钥

```bash
cd /opt/thesis
cp deploy/.env.example deploy/.env
vim deploy/.env   # 填写真实密钥
```

`.env` 需配置：
- `MYSQL_ROOT_PASSWORD`：MySQL root 密码（强密码）
- `MAIL_PASSWORD`：QQ 邮箱 SMTP 授权码
- `JWT_SECRET`：JWT 密钥，用 `openssl rand -base64 32` 生成

### 3. 构建并启动

```bash
cd /opt/thesis/deploy
docker compose up -d --build
```

### 4. 验证

```bash
# 查看容器状态
docker compose ps

# 查看日志
docker compose logs -f backend

# 健康检查
curl http://localhost/api/health
```

访问 `http://你的服务器IP` 即可。

## 三、HTTPS 配置（可选但推荐）

### 方式 1：Nginx 内置证书（简单）

1. 用 certbot 生成证书：
```bash
apt install certbot
certbot certonly --standalone -d your-domain.com
```
2. 将证书复制到 `deploy/certs/`
3. 修改 `deploy/nginx.conf`，取消注释 443 server 块
4. 取消 `docker-compose.yml` 中 frontend 的 443 端口映射注释
5. 重启：`docker compose up -d --build`

### 方式 2：前置 Nginx（推荐，不侵入容器）

服务器另装 Nginx 做反向代理 + SSL 终结，转发到 Docker 的 80 端口。

## 四、数据备份

### 数据库
```bash
docker exec thesis-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD thesis_format > backup_$(date +%Y%m%d).sql
```

### 上传文件
```bash
docker cp thesis-backend:/app/data/storage ./storage_backup
```

建议定时任务（crontab）每天备份。

## 五、常见问题

| 问题 | 解决 |
|---|---|
| 后端连不上 MySQL | 检查 `.env` 的 `MYSQL_ROOT_PASSWORD` 与 compose 一致 |
| 上传大文件 413 | Nginx `client_max_body_size` 已设为 100m，若更大需调整 |
| 端口冲突 | 修改 `docker-compose.yml` frontend 的 `80:80` 映射 |
| 邮箱验证码发不出 | 确认 `MAIL_PASSWORD` 是 SMTP 授权码（非 QQ 密码） |

## 六、更新部署

```bash
cd /opt/thesis
git pull   # 拉取最新代码
cd deploy
docker compose up -d --build   # 重新构建并滚动更新
```
