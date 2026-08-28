package com.museum.ticket.order;

import java.time.LocalDateTime;

public record OrderStatusResponse(
        String orderId,
        String status,
        String payStatus,
        String message,
        LocalDateTime operatedAt
) {
}
