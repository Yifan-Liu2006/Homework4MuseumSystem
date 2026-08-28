package com.museum.ticket.ticketing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketAvailabilityResponseTest {
    @Test
    void calculatesAvailableQuantity() {
        TicketAvailabilityResponse.TicketAvailability ticket =
                TicketAvailabilityResponse.TicketAvailability.of(
                        "ST1", "TT1", "成人票", new BigDecimal("50.00"), null, 100, 20, 5);

        assertEquals(75, ticket.availableQuantity());
    }

    @Test
    void neverReturnsNegativeAvailability() {
        TicketAvailabilityResponse.TicketAvailability ticket =
                TicketAvailabilityResponse.TicketAvailability.of(
                        "ST1", "TT1", "成人票", new BigDecimal("50.00"), null, 10, 8, 5);

        assertEquals(0, ticket.availableQuantity());
    }
}
