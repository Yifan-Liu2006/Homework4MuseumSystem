package com.museum.ticket.ticketing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TicketAvailabilityResponse(
        String openDayId,
        LocalDate visitDate,
        boolean holiday,
        List<SlotAvailability> slots
) {
    public record SlotAvailability(
            String slotId,
            String slotCode,
            LocalTime checkinStart,
            LocalTime checkinEnd,
            List<TicketAvailability> tickets
    ) {
    }

    public record TicketAvailability(
            String stockId,
            String ticketTypeId,
            String name,
            BigDecimal price,
            String description,
            int totalQuantity,
            int soldQuantity,
            int lockedQuantity,
            int availableQuantity
    ) {
        public static TicketAvailability of(String stockId, String ticketTypeId, String name, BigDecimal price,
                                            String description, int totalQuantity, int soldQuantity,
                                            int lockedQuantity) {
            int availableQuantity = Math.max(0, totalQuantity - soldQuantity - lockedQuantity);
            return new TicketAvailability(stockId, ticketTypeId, name, price, description, totalQuantity,
                    soldQuantity, lockedQuantity, availableQuantity);
        }
    }
}
