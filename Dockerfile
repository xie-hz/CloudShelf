# 后端 Dockerfile - Spring Boot + Java 8 + ffmpeg
FROM maven:3.8-openjdk-8 as builder

WORKDIR /build
COPY pom.xml .
# 先下载依赖，利用 Docker 缓存层
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# 运行阶段
FROM openjdk:8-jre-slim

# 安装 ffmpeg（视频处理需要）
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 创建数据目录
RUN mkdir -p /app/data/file /app/data/temp /app/data/avatar /app/data/logs

# 从构建阶段复制 jar
COPY --from=builder /build/target/cloudshelf-1.0.jar /app/app.jar

# 暴露端口
EXPOSE 7090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]