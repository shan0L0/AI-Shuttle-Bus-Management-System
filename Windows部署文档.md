# 智能车厂管理系统 - Windows 部署指南

## 目录

- [环境要求](#环境要求)
- [环境安装](#环境安装)
- [项目部署](#项目部署)
- [依赖包说明](#依赖包说明)
- [常见问题解决](#常见问题解决)
- [配置说明](#配置说明)

---

## 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 推荐使用 Oracle JDK 17 或 Amazon Corretto 17 |
| Node.js | 18+ | 推荐使用 LTS 版本 (18.x 或 20.x) |
| Maven | 3.8+ | Java 构建工具 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.0+ | 缓存服务（Windows 版本） |
| Git | 最新版 | 版本控制（可选） |

---

## 环境安装

### 1. 安装 JDK 17

**方式一：Oracle JDK**

1. 访问 [Oracle JDK 下载页面](https://www.oracle.com/java/technologies/downloads/#java17)
2. 下载 Windows x64 Installer（.exe 或 .msi）
3. 运行安装程序，按提示完成安装
4. 配置环境变量：
   - `JAVA_HOME` = `C:\Program Files\Java\jdk-17`
   - 将 `%JAVA_HOME%\bin` 添加到 `Path`

**方式二：Amazon Corretto（推荐）**

1. 访问 [Amazon Corretto 下载页面](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
2. 下载 Windows x64 MSI 安装包
3. 运行安装程序（自动配置环境变量）

**验证安装：**
```cmd
java -version
```
应显示 `java version "17.x.x"` 或类似信息。

---

### 2. 安装 Maven

1. 访问 [Maven 官网](https://maven.apache.org/download.cgi)
2. 下载 `apache-maven-x.x.x-bin.zip`
3. 解压到 `C:\Program Files\Apache\maven`
4. 配置环境变量：
   - `MAVEN_HOME` = `C:\Program Files\Apache\maven`
   - 将 `%MAVEN_HOME%\bin` 添加到 `Path`

**验证安装：**
```cmd
mvn -version
```

**配置国内镜像加速（重要）：**

编辑 `%MAVEN_HOME%\conf\settings.xml` 或 `%USERPROFILE%\.m2\settings.xml`，在 `<mirrors>` 标签内添加：

```xml
<mirrors>
    <!-- 阿里云公共仓库 -->
    <mirror>
        <id>aliyunmaven</id>
        <mirrorOf>*</mirrorOf>
        <name>阿里云公共仓库</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

---

### 3. 安装 Node.js

1. 访问 [Node.js 官网](https://nodejs.org/)
2. 下载 LTS 版本（推荐 18.x 或 20.x）的 Windows Installer (.msi)
3. 运行安装程序，勾选 "Automatically install the necessary tools"
4. 安装完成后重启命令行

**验证安装：**
```cmd
node -v
npm -v
```

**配置国内镜像加速（重要）：**

```cmd
# 设置淘宝镜像
npm config set registry https://registry.npmmirror.com

# 验证配置
npm config get registry
```

---

### 4. 安装 MySQL 8.0

**方式一：MySQL Installer（推荐）**

1. 访问 [MySQL 下载页面](https://dev.mysql.com/downloads/installer/)
2. 下载 MySQL Installer for Windows
3. 运行安装程序，选择 "Developer Default" 或 "Server only"
4. 设置 root 密码为 `root123456`（与配置文件一致）
5. 完成安装

**方式二：解压版**

1. 下载 ZIP Archive 版本
2. 解压到 `C:\mysql`
3. 创建 `my.ini` 配置文件
4. 初始化数据库并启动服务

**验证安装：**
```cmd
mysql -u root -p
```

---

### 5. 安装 Redis（Windows 版本）

**方式一：使用 Memurai（推荐）**

Memurai 是 Redis 的 Windows 原生替代品：

1. 访问 [Memurai 官网](https://www.memurai.com/)
2. 下载免费开发者版本
3. 安装并启动服务

**方式二：使用 Redis Windows 版**

1. 访问 [tporadowski/redis Releases](https://github.com/tporadowski/redis/releases)
2. 下载 `Redis-x64-x.x.xxx.zip`
3. 解压到 `C:\redis`
4. 运行 `redis-server.exe`

**方式三：使用 WSL2（Windows 10/11）**

```powershell
# 在 PowerShell 中以管理员身份运行
wsl --install

# 安装完成后，在 WSL 中安装 Redis
sudo apt update
sudo apt install redis-server
sudo service redis-server start
```

**验证安装：**
```cmd
redis-cli ping
```
应返回 `PONG`。

---

## 项目部署

### 步骤 1：解压项目文件

将 `智能车厂管理系统_Claude完整代码.zip` 解压到目标目录，例如：
```
D:\Projects\smart-shuttle\
```

### 步骤 2：初始化数据库

1. 启动 MySQL 服务
2. 打开命令行或 MySQL Workbench
3. 执行初始化脚本：

```cmd
# 使用命令行
mysql -u root -p < D:\Projects\smart-shuttle\sql\init.sql
```

或在 MySQL 客户端中执行：
```sql
source D:/Projects/smart-shuttle/sql/init.sql
```

### 步骤 3：启动 Redis

```cmd
# 如果使用解压版 Redis
cd C:\redis
redis-server.exe

# 或启动 Memurai/Redis 服务
net start Redis
```

### 步骤 4：构建并启动后端

打开新的命令行窗口：

```cmd
# 进入后端目录
cd D:\Projects\smart-shuttle\backend

# 清理并安装依赖（首次运行需要较长时间）
mvn clean install -DskipTests

# 进入启动模块
cd smart-shuttle-admin

# 启动后端服务
mvn spring-boot:run
```

> **注意：** 首次运行 `mvn clean install` 会下载大量依赖包，请确保网络通畅并配置好 Maven 镜像。

**后端启动成功标志：**
```
Started SmartShuttleApplication in x.xxx seconds
```

**后端服务地址：**
- API 服务: http://localhost:8080
- API 文档: http://localhost:8080/doc.html

### 步骤 5：安装并启动前端

打开新的命令行窗口：

```cmd
# 进入前端目录
cd D:\Projects\smart-shuttle\frontend

# 安装依赖（首次运行需要较长时间）
npm install

# 启动开发服务器
npm run dev
```

**前端启动成功标志：**
```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

**前端访问地址：** http://localhost:3000

### 步骤 6：访问系统

- **前端地址：** http://localhost:3000
- **API 文档：** http://localhost:8080/doc.html
- **默认账号：** admin / admin123

---

## 依赖包说明

### 后端依赖（Maven）

| 依赖包 | 版本 | 用途 | Maven 仓库 |
|--------|------|------|------------|
| spring-boot-starter-web | 3.2.1 | Web 框架 | ✅ 公共仓库 |
| spring-boot-starter-security | 3.2.1 | 安全框架 | ✅ 公共仓库 |
| spring-boot-starter-data-redis | 3.2.1 | Redis 集成 | ✅ 公共仓库 |
| spring-boot-starter-validation | 3.2.1 | 参数校验 | ✅ 公共仓库 |
| mybatis-plus-spring-boot3-starter | 3.5.5 | ORM 框架 | ✅ 公共仓库 |
| mysql-connector-j | 自动管理 | MySQL 驱动 | ✅ 公共仓库 |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.3 | JWT 认证 | ✅ 公共仓库 |
| knife4j-openapi3-jakarta-spring-boot-starter | 4.3.0 | API 文档 | ✅ 公共仓库 |
| hutool-all | 5.8.24 | 工具类库 | ✅ 公共仓库 |
| okhttp | 4.12.0 | HTTP 客户端 | ✅ 公共仓库 |
| lombok | 自动管理 | 代码简化 | ✅ 公共仓库 |

> 所有后端依赖均可从 Maven 中央仓库或阿里云镜像下载，无特殊依赖。

### 前端依赖（npm）

| 依赖包 | 版本 | 用途 | npm 仓库 |
|--------|------|------|----------|
| vue | ^3.4.0 | 前端框架 | ✅ npm 公共仓库 |
| vue-router | ^4.2.5 | 路由管理 | ✅ npm 公共仓库 |
| pinia | ^2.1.7 | 状态管理 | ✅ npm 公共仓库 |
| axios | ^1.6.2 | HTTP 请求 | ✅ npm 公共仓库 |
| element-plus | ^2.4.4 | UI 组件库 | ✅ npm 公共仓库 |
| @element-plus/icons-vue | ^2.3.1 | 图标库 | ✅ npm 公共仓库 |
| echarts | ^5.4.3 | 图表库 | ✅ npm 公共仓库 |
| @amap/amap-jsapi-loader | ^1.0.1 | 高德地图 | ✅ npm 公共仓库 |
| dayjs | ^1.11.10 | 日期处理 | ✅ npm 公共仓库 |
| marked | ^11.1.0 | Markdown 解析 | ✅ npm 公共仓库 |
| highlight.js | ^11.9.0 | 代码高亮 | ✅ npm 公共仓库 |
| vite | ^5.0.8 | 构建工具 | ✅ npm 公共仓库 |
| sass | ^1.69.5 | CSS 预处理 | ✅ npm 公共仓库 |

> 所有前端依赖均可从 npm 公共仓库或淘宝镜像下载，无特殊依赖。

---

## 常见问题解决

### 问题 1：Maven 下载依赖超时

**现象：** `mvn clean install` 卡住或报网络错误

**解决方案：**
1. 确认已配置阿里云镜像（见上文 Maven 安装部分）
2. 检查网络代理设置
3. 清除本地缓存重试：
```cmd
rmdir /s /q %USERPROFILE%\.m2\repository
mvn clean install -DskipTests
```

### 问题 2：npm install 失败

**现象：** 安装依赖时报错，下载超时

**解决方案：**
```cmd
# 清除 npm 缓存
npm cache clean --force

# 删除 node_modules 和 lock 文件
rmdir /s /q node_modules
del package-lock.json

# 使用淘宝镜像重新安装
npm install --registry=https://registry.npmmirror.com
```

### 问题 3：Sass 安装失败

**现象：** `npm install` 时 sass 相关错误

**解决方案：**
```cmd
# 方式一：安装 Windows 构建工具
npm install --global --production windows-build-tools

# 方式二：使用 dart-sass（通常自动使用）
npm install sass@latest --save-dev
```

### 问题 4：MySQL 连接失败

**现象：** 后端启动报数据库连接错误

**解决方案：**
1. 确认 MySQL 服务已启动：
```cmd
net start MySQL80
```
2. 检查用户名密码是否正确（默认 root/root123456）
3. 修改 `application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_shuttle?...
    username: 你的用户名
    password: 你的密码
```

### 问题 5：Redis 连接失败

**现象：** 后端启动报 Redis 连接错误

**解决方案：**
1. 确认 Redis 服务已启动
2. 如果 Redis 设置了密码，修改 `application.yml`：
```yaml
spring:
  data:
    redis:
      password: 你的Redis密码
```

### 问题 6：端口被占用

**现象：** 启动时报端口占用错误

**解决方案：**
```cmd
# 查看端口占用（以 8080 为例）
netstat -ano | findstr :8080

# 结束占用进程（PID 为上一步显示的进程ID）
taskkill /PID <PID> /F
```

或修改端口配置：
- 后端：修改 `application.yml` 中的 `server.port`
- 前端：修改 `vite.config.js` 中的 `server.port`

### 问题 7：跨域问题

**现象：** 前端请求后端报 CORS 错误

**解决方案：**
确保前端代理配置正确，检查 `vite.config.js`：
```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

---

## 配置说明

### 后端配置文件

位置：`backend/smart-shuttle-admin/src/main/resources/application.yml`

#### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_shuttle?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root123456
```

#### Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:    # 无密码则留空
      database: 0
```

#### AI 配置
```yaml
ai:
  # 可选：deepseek / qwen / wenxin
  provider: deepseek
  
  deepseek:
    api-key: 你的API密钥
    model: deepseek-chat
    
  qwen:
    api-key: 你的API密钥
    model: qwen-turbo
    
  wenxin:
    api-key: 你的API密钥
    secret-key: 你的Secret密钥
    model: ernie-bot-4
```

### 前端配置文件

位置：`frontend/vite.config.js`

#### 开发服务器配置
```javascript
server: {
  port: 3000,           // 前端端口
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true
    }
  }
}
```

---

## 生产环境打包

### 后端打包

```cmd
cd backend
mvn clean package -DskipTests
```

生成的 JAR 文件位于：`backend/smart-shuttle-admin/target/smart-shuttle-admin-1.0.0.jar`

运行：
```cmd
java -jar smart-shuttle-admin-1.0.0.jar
```

### 前端打包

```cmd
cd frontend
npm run build
```

生成的静态文件位于：`frontend/dist/`

部署时将 `dist` 目录下的文件部署到 Nginx 或其他 Web 服务器。

---

## 快速启动脚本

创建 `start.bat` 批处理文件，一键启动所有服务：

```batch
@echo off
echo ========================================
echo    智能车厂管理系统 - 启动脚本
echo ========================================

echo.
echo [1/3] 启动 Redis...
start "Redis" cmd /k "cd /d C:\redis && redis-server.exe"
timeout /t 3

echo.
echo [2/3] 启动后端服务...
start "Backend" cmd /k "cd /d %~dp0backend\smart-shuttle-admin && mvn spring-boot:run"
timeout /t 30

echo.
echo [3/3] 启动前端服务...
start "Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"

echo.
echo ========================================
echo    所有服务已启动！
echo    前端地址: http://localhost:3000
echo    API文档:  http://localhost:8080/doc.html
echo    默认账号: admin / admin123
echo ========================================
pause
```

将此文件保存到项目根目录，双击即可启动所有服务。

---

## 联系支持

如遇到其他问题，请检查：
1. 所有服务是否正常启动
2. 端口是否被占用
3. 配置文件是否正确
4. 网络是否能访问 Maven/npm 仓库

---

**文档版本：** v1.0  
**最后更新：** 2024年12月
