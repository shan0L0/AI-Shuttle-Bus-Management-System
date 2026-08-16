# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制pom文件
COPY pom.xml .
COPY smart-shuttle-common/pom.xml smart-shuttle-common/
COPY smart-shuttle-system/pom.xml smart-shuttle-system/
COPY smart-shuttle-business/pom.xml smart-shuttle-business/
COPY smart-shuttle-ai/pom.xml smart-shuttle-ai/
COPY smart-shuttle-admin/pom.xml smart-shuttle-admin/

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY . .

# 构建
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制构建产物
COPY --from=builder /app/smart-shuttle-admin/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
