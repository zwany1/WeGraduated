# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-11 AS builder
WORKDIR /build
# 使用阿里云 Maven 镜像加速依赖下载(服务器访问中央仓库慢)
RUN mkdir -p /root/.m2 && \
    printf '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><name>Aliyun Maven</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B -q || true
COPY backend/src ./src
RUN mvn package -DskipTests -B -q

# ---------- 运行阶段 ----------
# 基于 Ubuntu + JDK11 + LibreOffice(PDF 转换依赖)
FROM eclipse-temurin:11-jre-focal
ENV DEBIAN_FRONTEND=noninteractive

# 安装 LibreOffice (docx -> PDF)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libreoffice-writer \
    libreoffice-core \
    fonts-wqy-microhei \
    fonts-wqy-zenhei \
    fontconfig \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 中文字体缓存
RUN fc-cache -f

WORKDIR /app
COPY --from=builder /build/target/thesis-format.jar /app/thesis-format.jar

# 存储目录
RUN mkdir -p /app/data/storage
VOLUME ["/app/data"]

# 环境变量注入密钥(生产必填: 数据库密码/邮箱授权码/JWT密钥)
ENV DB_USERNAME=root \
    DB_PASSWORD= \
    DB_URL=jdbc:mysql://mysql:3306/thesis_format?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true \
    MAIL_PASSWORD= \
    JWT_SECRET= \
    STORAGE_DIR=/app/data/storage

EXPOSE 13355

CMD ["java", "-jar", "/app/thesis-format.jar"]
