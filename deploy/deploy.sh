#!/bin/bash
# ============================================
# 论文排版系统 - 一键部署脚本
# 适用: Alibaba Cloud Linux 3 / CentOS 7 / CentOS 8
# 用法: bash deploy.sh
# ============================================
set -e

echo "=========== 1/6 更新系统 ==========="
yum update -y || true

echo "=========== 2/6 安装 Docker ==========="
if ! command -v docker &>/dev/null; then
    yum install -y yum-utils
    yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo 2>/dev/null || \
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    yum install -y docker-ce docker-ce-cli containerd.io
    systemctl start docker
    systemctl enable docker
fi
echo "Docker: $(docker --version)"

echo "=========== 3/6 安装 Docker Compose ==========="
if ! command -v docker-compose &>/dev/null; then
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
fi
echo "Compose: $(docker-compose --version)"

echo "=========== 4/6 配置密钥 ==========="
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

if [ ! -f deploy/.env ]; then
    cp deploy/.env.example deploy/.env
    echo "已生成 deploy/.env，请编辑填写密钥！"
    echo "  MYSQL_ROOT_PASSWORD: 数据库密码"
    echo "  MAIL_PASSWORD: QQ邮箱SMTP授权码"
    echo "  JWT_SECRET: openssl rand -base64 32 生成"
    echo "填写后重新运行: bash deploy/deploy.sh"
    exit 0
fi

echo "=========== 5/6 构建并启动 ==========="
docker-compose -f deploy/docker-compose.yml up -d --build

echo "=========== 6/6 验证 ==========="
sleep 5
echo "容器状态:"
docker-compose -f deploy/docker-compose.yml ps
echo ""
echo "健康检查:"
curl -s http://localhost/api/health && echo "" || echo "后端启动中，稍后重试"

echo ""
echo "=========================================="
echo "部署完成！访问 http://服务器IP"
echo "=========================================="
