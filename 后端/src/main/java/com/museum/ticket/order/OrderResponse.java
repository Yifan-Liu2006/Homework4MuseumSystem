package com.museum.ticket.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record OrderResponse(
        String orderId,
        LocalDate visitDate,
        BigDecimal price,
        LocalDateTime orderDate,
        String status,
        String payStatus,
        LocalDateTime paymentDeadline,
        List<OrderDetailResponse> details
) {
    public record OrderDetailResponse(
            String detailId,
            String personId,
            String personName,
            String idMasked,
            String stockId,
            String ticketTypeName,
            String slotCode,
            LocalTime checkinStart,
            LocalTime checkinEnd,
            BigDecimal price,
            String verifyStatus
    ) {
    }
}
