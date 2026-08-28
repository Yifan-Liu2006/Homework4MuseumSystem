package com.museum.ticket.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.ticket.common.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService(
            new ObjectMapper(), "test-secret-with-at-least-32-characters", 7200);

    @Test
    void createsAndParsesToken() {
        String token = jwtService.createToken("V123456789");

        assertEquals("V123456789", jwtService.parseVisitorId(token));
    }

    @Test
    void rejectsModifiedToken() {
        String token = jwtService.createToken("V123456789");
        String modifiedToken = token.substring(0, token.length() - 1) + "x";

        assertThrows(UnauthorizedException.class, () -> jwtService.parseVisitorId(modifiedToken));
    }
}
