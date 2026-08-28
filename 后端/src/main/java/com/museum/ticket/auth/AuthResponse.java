package com.museum.ticket.auth;

import java.time.LocalDateTime;

public record AuthResponse(
        String visitorId,
        String mobile,
        String status,
        LocalDateTime registerTime,
        LocalDateTime lastLoginTime
) {
}
