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
| GET | `/api/ticketing/availability` | 公开查询可预约日期、场次、票种和库存 |

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

票务查询支持可选的 `from` 和 `to` 参数，格式为 `yyyy-MM-dd`。不传参数时查询今天起未来 30 天，单次范围不能超过 90 天：

```text
GET /api/ticketing/availability?from=2026-09-01&to=2026-09-30
```

接口只返回已开票、未闭馆、已经到达发布时间的开放日，以及启用场次和上架票种。可用库存等于总库存减去已售和锁定库存。

当前注册登录、JWT、游客信息、实名人员管理和公开票务查询已经完成，并通过 7 项单元测试。下一步实现事务下单和游客订单查询。
