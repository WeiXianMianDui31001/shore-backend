# 岸上见 - 后端服务

## 技术栈

- Spring Boot 3.2.5
- Java 17
- Maven
- PostgreSQL
- Redis
- Spring Security + JWT
- MyBatis-Plus 3.5.7
- Spring WebSocket

## 项目结构

```
backend/
├── pom.xml
├── src/main/java/com/anzs/
│   ├── ShoreBackendApplication.java
│   ├── common/               # 通用工具、异常、响应封装
│   ├── config/               # 配置类（Security、MyBatis-Plus、WebSocket）
│   ├── infrastructure/       # Redis 缓存封装
│   └── module/               # 业务模块
│       ├── auth/             # 认证模块（注册、登录、验证码、找回密码）
│       ├── user/             # 用户模块（个人信息、积分、收藏）
│       ├── resource/         # 资源模块（上传、下载、列表）
│       ├── community/        # 社区模块（帖子、评论）
│       ├── info/             # 信息聚合模块
│       ├── notification/     # 通知模块
│       ├── room/             # 讨论室模块（含 WebSocket 实时通信）
│       ├── resume/           # 简历模块（模板、简历、导出）
│       └── admin/            # 管理后台模块
└── src/main/resources/
    ├── application.yml       # 应用配置
    └── schema.sql            # 数据库初始化脚本
```

## 快速启动

### 1. 环境准备

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+
- Redis 6+



### 2. 执行初始化脚本

在docker文件目录下运行docker compose up -d后在idea中启动程序即可
### 3. 修改配置

编辑 `src/main/resources/application.yml`（不用动）



## 默认管理员

- 邮箱：`admin@anzs.com`
- 密码：`admin123`

## API 前缀

所有接口前缀：`/api/v1`

WebSocket 地址：`ws://localhost:8080/ws/room/{roomId}?token={JWT}`

