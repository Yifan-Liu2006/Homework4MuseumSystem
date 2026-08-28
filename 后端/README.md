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
| POST | `/api/auth/login` | 校验游客信息并签发 JWT |
| GET | `/api/visitors/me` | 使用 JWT 查询当前游客 |

注册和登录请求示例：

```json
{
  "mobile": "13800138000",
  "password": "Museum123"
}
```

登录响应中的 `token` 需要通过请求头发送给受保护接口：

```text
Authorization: Bearer <token>
```

JWT 默认有效期为 7200 秒，可用 `JWT_EXPIRATION_SECONDS` 调整。部署时必须用至少 32 个字符的随机 `JWT_SECRET` 替换开发默认值。

当前注册、登录、JWT 鉴权和游客信息查询已经完成，并通过 2 项 JWT 单元测试。下一步实现实名参观人员增删改查、证件哈希和脱敏。
