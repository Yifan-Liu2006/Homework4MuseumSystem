package com.museum.ticket.admin;

public record AdminLoginResponse(String token, String tokenType, long expiresIn, AdminResponse admin) {
}
