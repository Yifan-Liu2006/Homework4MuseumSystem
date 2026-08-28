# 博物馆订票系统后端

技术栈：Java 25、Spring Boot 3.5、MyBatis-Plus、MySQL 8。

## 启动

先执行 `../数据库层/museum_ticket_schema.sql` 初始化数据库，然后配置数据库环境变量并运行：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
.\mvnw.cmd spring-boot:run
```

启动后访问 `GET http://localhost:8080/api/system/health`，返回 `database: UP` 即表示连接成功。也可以通过 `DB_URL`、`DB_POOL_SIZE` 和 `SERVER_PORT` 覆盖默认配置。

## 已实现接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/system/health` | 检查后端和数据库状态 |
| POST | `/api/auth/register` | 使用手机号和密码注册游客 |
| POST | `/api/auth/login` | 校验游客手机号、密码和账号状态 |

注册和登录请求示例：

```json
{
  "mobile": "13800138000",
  "password": "Museum123"
}
```

当前注册和登录功能已完成，并已使用 Java 25 + Maven 编译通过；下一步实现 JWT 令牌、接口鉴权和当前游客信息查询。若启动时需要下载依赖，请确保 Maven 可以访问中央仓库。
