package com.museum.ticket.admin;

import com.museum.ticket.common.BusinessException;
import com.museum.ticket.common.UnauthorizedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminAuthService {
    private final JdbcTemplate jdbcTemplate;
    private final AdminJwtService adminJwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(JdbcTemplate jdbcTemplate, AdminJwtService adminJwtService) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminJwtService = adminJwtService;
    }

    public AdminLoginResponse login(AdminLoginRequest request, String ipAddress) {
        List<AdminRecord> records = findByAccount(request.account());
        if (records.isEmpty() || !passwordEncoder.matches(request.password(), records.getFirst().passwordHash())) {
            throw new UnauthorizedException("管理员账号或密码错误");
        }
        AdminRecord record = records.getFirst();
        if (!"正常".equals(record.status())) {
            throw new UnauthorizedException("管理员账号已被禁用");
        }
        AdminCurrent.AdminPrincipal principal = new AdminCurrent.AdminPrincipal(
                record.workerId(), record.roleId(), record.roleName());
        writeLog(record.workerId(), "管理员登录", record.account(), "成功", ipAddress);
        return new AdminLoginResponse(adminJwtService.create(principal), "Bearer",
                adminJwtService.expirationSeconds(), toResponse(record));
    }

    public AdminResponse current() {
        AdminCurrent.AdminPrincipal principal = AdminCurrent.require();
        List<AdminRecord> records = jdbcTemplate.query("""
                SELECT w.workerID, w.account, w.password_hash, w.name, w.status,
                       r.roleID, r.role_name, r.permission_description
                FROM worker w JOIN role r ON r.roleID = w.roleID WHERE w.workerID = ?
                """, (resultSet, rowNumber) -> map(resultSet), principal.workerId());
        if (records.isEmpty() || !"正常".equals(records.getFirst().status())) {
            throw new UnauthorizedException("管理员账号不存在或已禁用");
        }
        return toResponse(records.getFirst());
    }

    private List<AdminRecord> findByAccount(String account) {
        return jdbcTemplate.query("""
                SELECT w.workerID, w.account, w.password_hash, w.name, w.status,
                       r.roleID, r.role_name, r.permission_description
                FROM worker w JOIN role r ON r.roleID = w.roleID WHERE w.account = ?
                """, (resultSet, rowNumber) -> map(resultSet), account);
    }

    private AdminRecord map(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new AdminRecord(resultSet.getString("workerID"), resultSet.getString("account"),
                resultSet.getString("password_hash"), resultSet.getString("name"),
                resultSet.getString("status"), resultSet.getString("roleID"),
                resultSet.getString("role_name"), resultSet.getString("permission_description"));
    }

    private AdminResponse toResponse(AdminRecord record) {
        return new AdminResponse(record.workerId(), record.account(), record.name(), record.status(),
                record.roleId(), record.roleName(), record.permissionDescription());
    }

    private void writeLog(String workerId, String type, String object, String result, String ipAddress) {
        jdbcTemplate.update("""
                INSERT INTO operation_log(logID, workerID, operation_type, operation_object, result, ip_address)
                VALUES (?, ?, ?, ?, ?, ?)
                """, AdminIdGenerator.generate("L"), workerId, type, object, result, ipAddress);
    }

    private record AdminRecord(String workerId, String account, String passwordHash, String name, String status,
                               String roleId, String roleName, String permissionDescription) {
    }
}
