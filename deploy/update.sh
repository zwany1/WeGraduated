#!/usr/bin/env bash
# ==========================================================
# 一键更新部署脚本（在服务器上执行）
# 用法: bash deploy/update.sh
#
# 功能:
#   1. 拉取最新代码 (git pull)
#   2. 若 /tmp/frontend-dist.tar.gz 存在, 则更新前端并重建前端镜像
#   3. 重建后端镜像并更新容器
#   4. 输出容器状态与健康检查
# ==========================================================
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "[1/4] 恢复 compose 为仓库版本并拉取最新代码..."
# 服务器上 docker-compose.yml 可能被历史手工修改过, 仓库版本即为所需版本
if [ -n "$(git status --porcelain deploy/docker-compose.yml)" ]; then
    echo "  检测到 deploy/docker-compose.yml 有本地修改, 已恢复为仓库版本"
    git checkout -- deploy/docker-compose.yml
fi
git pull --ff-only

echo "[2/4] 更新前端(如有新上传的 dist 包)..."
DIST_TAR=/tmp/frontend-dist.tar.gz
if [ -f "$DIST_TAR" ]; then
    echo "  检测到 $DIST_TAR, 解压并重建前端镜像..."
    rm -rf frontend/dist
    mkdir -p frontend/dist
    tar -xzf "$DIST_TAR" -C frontend/
    mkdir -p dist_stage
    cp -r frontend/dist dist_stage/dist
    cp deploy/nginx.conf dist_stage/nginx.conf
    cat > dist_stage/Dockerfile <<'DOCKER'
FROM nginx:alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
DOCKER
    (cd dist_stage && docker build -t deploy-frontend .)
    docker compose -f deploy/docker-compose.yml up -d frontend
else
    echo "  未发现 $DIST_TAR, 跳过前端更新(前端保持现有镜像)"
fi

echo "[3/4] 重建后端并更新容器..."
docker compose -f deploy/docker-compose.yml up -d --build backend

echo "[4/4] 检查状态..."
sleep 3
docker compose -f deploy/docker-compose.yml ps
echo "--- 健康检查 ---"
curl -s http://localhost/api/health && echo ""
echo "更新完成"
