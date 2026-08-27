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
