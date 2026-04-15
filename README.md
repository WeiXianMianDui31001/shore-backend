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

### 2. 创建数据库

```sql
CREATE DATABASE shore_db;
```

### 3. 执行初始化脚本

运行 `src/main/resources/schema.sql` 创建表结构并插入默认数据：

- 管理员账号：`admin@anzs.com` / `admin123`
- 初始积分规则

### 4. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shore_db?currentSchema=public&stringtype=unspecified
    username: postgres
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password  # 若无密码留空
```

### 5. 编译运行

```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run
```

服务默认启动在 `http://localhost:8080`

## 默认管理员

- 邮箱：`admin@anzs.com`
- 密码：`admin123`

## API 前缀

所有接口前缀：`/api/v1`

WebSocket 地址：`ws://localhost:8080/ws/room/{roomId}?token={JWT}`

## 注意事项

1. **JWT 密钥**：生产环境请务必修改 `application.yml` 中的 `shore.jwt.secret`
2. **文件上传**：当前 `prepareUpload` 为简化实现，生产环境建议对接 OSS（如 MinIO、阿里云 OSS）生成预签名 URL
3. **邮件发送**：验证码邮件发送逻辑目前为 TODO，需集成邮件服务（如 JavaMail、SendGrid）
4. **PDF 导出**：简历 PDF 导出目前仅创建记录，需补充异步生成逻辑
5. **编译环境**：若本地无 Maven，可使用 IDE（IntelliJ IDEA / VS Code）导入后运行
