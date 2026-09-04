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

echo "[1/4] 恢复部署文件为仓库版本并拉取最新代码..."
# 服务器上这些文件可能被构建/手工操作污染, 仓库版本即为所需版本
for f in deploy/docker-compose.yml frontend/index.html; do
    if [ -n "$(git status --porcelain "$f")" ]; then
        echo "  检测到 $f 有本地修改, 已恢复为仓库版本"
        git checkout -- "$f"
    fi
done
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
# 3a: 执行增量迁移(幂等,重复执行安全); 从 thesis-mysql 容器内读取 MYSQL_ROOT_PASSWORD
if [ -d "$PROJECT_ROOT/deploy/migrations" ]; then
  MYSQL_PWD=$(docker exec thesis-mysql printenv MYSQL_ROOT_PASSWORD)
  for sql in $(ls "$PROJECT_ROOT/deploy/migrations/"*.sql 2>/dev/null | sort); do
    echo "  应用迁移: $(basename "$sql")"
    docker exec -i -e MYSQL_PWD="$MYSQL_PWD" thesis-mysql \
      sh -c 'mysql -uroot -p"$MYSQL_PWD" thesis_format' < "$sql" || {
        echo "  ! 迁移失败: $(basename "$sql"), 终止"; exit 1;
      }
  done
fi
# 3b: 重建后端镜像并启动新容器
docker compose -f deploy/docker-compose.yml up -d --build backend

echo "[4/4] 检查状态..."
docker compose -f deploy/docker-compose.yml ps
echo "--- 健康检查 ---"
# 轮询等待后端启动完成(最多 60s), 再探前端可达
for i in $(seq 1 30); do
  if docker logs thesis-backend 2>&1 | grep -q "Started ThesisApplication"; then
    echo "backend ready"
    break
  fi
  sleep 2
done
curl -s -o /dev/null -w "frontend http=%{http_code}\n" --max-time 5 http://localhost/
echo "更新完成"
