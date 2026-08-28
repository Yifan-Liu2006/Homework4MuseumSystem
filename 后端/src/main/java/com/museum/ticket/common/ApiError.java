package com.museum.ticket.common;

import java.time.LocalDateTime;

public record ApiError(int status, String message, LocalDateTime timestamp) {
}
