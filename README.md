# 🚌 智能车厂管理系统

> AI赋能的企业班车通勤管理解决方案

## 📋 项目简介

智能车厂管理系统是一套完整的企业班车通勤管理解决方案，集成了AI智能分析、地图可视化、数据报表等功能，帮助企业高效管理班车运营。

### 核心功能

- 📊 **数据总览** - 实时监控班车运营状态、乘坐率趋势
- 🚐 **车辆管理** - 车辆信息维护、状态监控、保养提醒
- 👥 **员工管理** - 员工通勤信息、乘车站点绑定
- 📍 **站点管理** - 高德地图可视化、站点分布展示
- 🛤️ **线路管理** - 线路规划、站点配置、乘坐率分析
- 🤖 **AI智能助手** - 自然语言交互、智能问答、数据查询
- ⚡ **智能优化** - 线路优化建议、成本分析、效率提升
- 📈 **数据报表** - 多维度统计分析、Excel导出

### AI能力支持

系统支持三种主流AI大模型：

| 模型 | 提供商 | 特点 |
|------|--------|------|
| DeepSeek | 深度求索 | 性价比高，响应快速 |
| Qwen | 阿里云 | 中文理解能力强 |
| ERNIE | 百度 | 知识丰富，稳定可靠 |

## 🛠️ 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS版本 |
| Spring Boot | 3.2.1 | 核心框架 |
| MyBatis Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.0+ | 缓存/Token |
| Knife4j | 4.3.0 | API文档 |
| JWT | 0.12.3 | 认证授权 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.x | 渐进式框架 |
| Element Plus | 2.4.x | UI组件库 |
| Vite | 5.0.x | 构建工具 |
| ECharts | 5.4.x | 图表库 |
| 高德地图 | 2.0 | 地图服务 |
| Pinia | 2.1.x | 状态管理 |

## 📁 项目结构

```
smart-shuttle/
├── backend/                          # 后端代码
│   ├── smart-shuttle-common/         # 公共模块
│   ├── smart-shuttle-system/         # 系统管理模块
│   ├── smart-shuttle-business/       # 业务模块
│   ├── smart-shuttle-ai/             # AI智能模块
│   └── smart-shuttle-admin/          # 启动模块
├── frontend/                         # 前端代码
│   ├── src/
│   │   ├── api/                      # API接口
│   │   ├── components/               # 公共组件
│   │   ├── views/                    # 页面
│   │   ├── router/                   # 路由
│   │   ├── store/                    # 状态管理
│   │   └── utils/                    # 工具函数
│   ├── package.json
│   └── vite.config.js
├── sql/                              # 数据库脚本
│   └── init.sql                      # 初始化SQL
├── docker-compose.yml                # Docker编排
└── README.md
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+

### 1. 数据库初始化

```bash
# 登录MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/sql/init.sql
```

### 2. 后端启动

```bash
cd backend

# 安装依赖
mvn clean install -DskipTests

# 启动服务
cd smart-shuttle-admin
mvn spring-boot:run
```

### 3. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

### 4. 访问系统

- 前端地址: http://localhost:3000
- API文档: http://localhost:8080/doc.html
- 默认账号: admin / admin123

## ⚙️ 配置说明

### AI配置

在 `application.yml` 中配置AI服务：

```yaml
ai:
  # 选择AI提供商: deepseek / qwen / wenxin
  provider: deepseek
  
  deepseek:
    api-key: your-api-key
    model: deepseek-chat
    
  qwen:
    api-key: your-api-key
    model: qwen-turbo
    
  wenxin:
    api-key: your-api-key
    secret-key: your-secret-key
    model: ernie-bot-4
```

### 地图配置

高德地图API Key配置：

```javascript
// vite.config.js 或直接在组件中配置
const AMAP_KEY = '63689a14fd13d6bb8dfdc069d48ac62d'
```

## 📊 API接口

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/auth/login | POST | 用户登录 |
| /api/v1/auth/logout | POST | 用户登出 |
| /api/v1/auth/refresh | POST | 刷新Token |

### 业务接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/vehicles | GET/POST | 车辆管理 |
| /api/v1/stations | GET/POST | 站点管理 |
| /api/v1/routes | GET/POST | 线路管理 |
| /api/v1/employees | GET/POST | 员工管理 |

### AI接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/ai/chat | POST | AI对话 |
| /api/v1/ai/optimize/routes | POST | 线路优化 |
| /api/v1/ai/schedule/advice | GET | 调度建议 |

## 🐳 Docker部署

```bash
# 使用docker-compose一键部署
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f app
```

## 📝 开发指南

### 添加新的AI提供商

1. 在 `smart-shuttle-ai` 模块创建新的Client类
2. 实现 `LlmClient` 接口
3. 在 `AiProperties` 中添加配置
4. 使用 `@ConditionalOnProperty` 注解条件加载

### 添加新的业务模块

1. 在 `smart-shuttle-business` 中创建子包
2. 创建 Entity、Mapper、Service、Controller
3. 在 `sql/init.sql` 中添加表结构
4. 前端添加对应页面和API

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

MIT License

## 👨‍💻 作者

SmartShuttle Team

---

🌟 如果这个项目对您有帮助，请给一个Star！
