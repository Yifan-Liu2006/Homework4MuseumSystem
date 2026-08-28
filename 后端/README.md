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
| POST | `/api/orders` | 当前游客事务下单，支持一单多票 |
| GET | `/api/orders` | 查询当前游客全部订单 |
| GET | `/api/orders/{orderId}` | 查询当前游客指定订单及明细 |
| POST | `/api/orders/{orderId}/pay` | 模拟支付待支付订单 |
| POST | `/api/orders/{orderId}/cancel` | 取消待支付订单并释放库存 |
| POST | `/api/orders/{orderId}/refund` | 退款并回退已售库存、作废凭证 |
| GET | `/api/vouchers` | 查询当前游客的电子凭证 |
| POST | `/api/verification/verify` | 使用独立核销密钥核销凭证 |

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

下单请求示例：

```json
{
  "items": [
    { "personId": "P...", "stockId": "ST..." },
    { "personId": "P...", "stockId": "ST..." }
  ]
}
```

订单金额由服务端从锁定后的票种数据计算，不接受客户端传入价格。一张订单最多 10 张票，只能包含同一天的门票；同一实名人员不能重复预约同一场次。待支付订单默认 15 分钟后过期。

模拟支付请求示例：

```json
{ "channel": "支付宝" }
```

支持 `微信支付`、`支付宝` 和 `其他`。支付成功会将对应数量从 `locked_quantity` 转入 `sold_quantity` 并写入成功支付记录。取消仅允许待支付订单；后台定时任务默认每 60 秒扫描并关闭超时待支付订单，可通过 `ORDER_EXPIRATION_SCAN_MILLISECONDS` 调整。

退款仅允许参观日期未过且没有任何已核验明细的已支付订单。退款会将 `sold_quantity` 扣回、订单与支付记录改为已退款、订单明细和电子凭证改为作废。

支付成功会为每条订单明细生成一个 64 位凭证码。核销请求不使用游客 JWT，而是在请求头中提供独立密钥：

```text
X-Verification-Key: <VERIFICATION_KEY>
```

```json
{
  "voucherCode": "64位凭证码",
  "workerId": null
}
```

部署时必须通过 `VERIFICATION_KEY` 设置至少 16 个字符的随机核销密钥。核销成功会将凭证改为已使用、订单明细改为已核验，并写入 `verification_record`；重复、作废或过期凭证返回失败结果。

当前游客侧核心后端闭环已经完成。下一步实现管理员登录、角色权限、票务配置、运营统计和操作日志。
