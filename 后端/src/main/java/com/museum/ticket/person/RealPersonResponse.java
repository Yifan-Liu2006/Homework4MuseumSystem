package com.museum.ticket.person;

import java.time.LocalDateTime;

public record RealPersonResponse(
        String personId,
        String name,
        String idType,
        String idMasked,
        boolean isSelf,
        LocalDateTime createdAt
) {
}
