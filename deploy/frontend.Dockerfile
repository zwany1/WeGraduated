# ---------- 构建阶段 ----------
FROM node:18-alpine AS builder
ENV NODE_OPTIONS=--max-old-space-size=1536
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci || npm install
# npm 可选依赖 bug(npm/cli#4828)导致 oxide 原生绑定缺失, 显式安装 musl 二进制
RUN npm install --no-save --no-package-lock @tailwindcss/oxide-linux-x64-musl@4.3.3
COPY frontend/ ./
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
