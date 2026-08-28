package com.museum.ticket.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.ticket.common.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminJwtServiceTest {
    private final AdminJwtService service = new AdminJwtService(
            new ObjectMapper(), "admin-test-secret-with-at-least-32-characters", 14400);

    @Test
    void createsAndParsesAdminToken() {
        AdminCurrent.AdminPrincipal principal =
                new AdminCurrent.AdminPrincipal("W001", "R001", "超级管理员");

        AdminCurrent.AdminPrincipal parsed = service.parse(service.create(principal));

        assertEquals(principal, parsed);
    }

    @Test
    void rejectsModifiedAdminToken() {
        String token = service.create(new AdminCurrent.AdminPrincipal("W001", "R001", "超级管理员"));
        String modified = token.substring(0, token.length() - 1) + "x";

        assertThrows(UnauthorizedException.class, () -> service.parse(modified));
    }
}
