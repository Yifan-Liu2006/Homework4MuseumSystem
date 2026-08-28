package com.museum.ticket.verification;

import java.time.LocalDateTime;

public record VerificationResponse(
        String voucherId,
        String result,
        String message,
        LocalDateTime verifiedAt
) {
}
