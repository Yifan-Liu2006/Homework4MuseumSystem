package com.museum.ticket.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final String account;
    private final String password;
    private final String name;

    public AdminBootstrap(JdbcTemplate jdbcTemplate,
                          @Value("${app.admin.bootstrap-account:admin}") String account,
                          @Value("${app.admin.bootstrap-password:}") String password,
                          @Value("${app.admin.bootstrap-name:系统管理员}") String name) {
        this.jdbcTemplate = jdbcTemplate;
        this.account = account;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (password.isBlank()) {
            return;
        }
        String roleId = "R_SUPER_ADMIN";
        jdbcTemplate.update("""
                INSERT INTO role(roleID, role_name, permission_description)
                VALUES (?, '超级管理员', 'ALL')
                ON DUPLICATE KEY UPDATE permission_description = VALUES(permission_description)
                """, roleId);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM worker WHERE account = ?", Integer.class, account);
        if (count != null && count == 0) {
            jdbcTemplate.update("""
                    INSERT INTO worker(workerID, account, password_hash, name, roleID, status)
                    VALUES (?, ?, ?, ?, ?, '正常')
                    """, AdminIdGenerator.generate("W"), account,
                    new BCryptPasswordEncoder().encode(password), name, roleId);
        }
    }
}
