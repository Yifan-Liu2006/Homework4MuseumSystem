package com.museum.ticket.auth;

public record LoginResponse(String token, String tokenType, long expiresIn, AuthResponse visitor) {
}
