package com.museum.ticket.voucher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record VoucherResponse(
        String voucherId,
        String voucherCode,
        String status,
        LocalDateTime generatedAt,
        LocalDateTime expiredAt,
        String orderId,
        LocalDate visitDate,
        String personName,
        String idMasked,
        String ticketTypeName,
        String slotCode,
        LocalTime checkinStart,
        LocalTime checkinEnd
) {
}
