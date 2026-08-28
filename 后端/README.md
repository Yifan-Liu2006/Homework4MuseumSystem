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
| GET | `/api/real-persons` | 查询当前游客的实名人员 |
| POST | `/api/real-persons` | 新增实名人员 |
| PUT | `/api/real-persons/{personId}` | 修改本人名下的实名人员 |
| DELETE | `/api/real-persons/{personId}` | 删除本人名下的实名人员 |

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

实名人员请求需要 `name`、`idType`、`idNumber` 和 `isSelf`。支持的证件类型为 `身份证`、`港澳台通行证` 和 `护照`。完整证件号码不会保存，数据库只保存分类 SHA-256 哈希和脱敏号码；每位游客最多设置一个“本人”实名信息。

当前注册、登录、JWT 鉴权、游客信息和实名人员管理已经完成，并通过 5 项单元测试。下一步实现开放日期、场次、票种和库存查询接口。
