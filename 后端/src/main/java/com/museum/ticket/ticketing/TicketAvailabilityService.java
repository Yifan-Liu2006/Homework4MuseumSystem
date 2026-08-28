package com.museum.ticket.ticketing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketAvailabilityService {
    private static final String AVAILABILITY_SQL = """
            SELECT d.openDayID, d.visit_date, d.is_holiday,
                   s.slotID, s.slot_code, s.checkin_start, s.checkin_end,
                   st.stockID, t.ticketTypeID, t.name AS ticket_name, t.price,
                   t.description, st.total_quantity, st.sold_quantity, st.locked_quantity
            FROM open_day d
            JOIN visit_slot s ON s.openDayID = d.openDayID AND s.status = '启用'
            JOIN ticket_stock st ON st.slotID = s.slotID
            JOIN ticket_type t ON t.ticketTypeID = st.ticketTypeID AND t.status = '上架'
            WHERE d.status = '已开票'
              AND d.is_closed = 0
              AND d.release_time <= NOW()
              AND d.visit_date BETWEEN ? AND ?
            ORDER BY d.visit_date, s.checkin_start, t.price, t.name
            """;

    private final JdbcTemplate jdbcTemplate;

    public TicketAvailabilityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TicketAvailabilityResponse> findAvailability(LocalDate from, LocalDate to) {
        Map<String, DayBuilder> days = new LinkedHashMap<>();
        jdbcTemplate.query(AVAILABILITY_SQL, resultSet -> {
            String openDayId = resultSet.getString("openDayID");
            LocalDate visitDate = resultSet.getDate("visit_date").toLocalDate();
            boolean holiday = resultSet.getBoolean("is_holiday");
            DayBuilder day = days.computeIfAbsent(openDayId,
                    ignored -> new DayBuilder(openDayId, visitDate, holiday));

            String slotId = resultSet.getString("slotID");
            String slotCode = resultSet.getString("slot_code");
            java.time.LocalTime checkinStart = resultSet.getTime("checkin_start").toLocalTime();
            java.time.LocalTime checkinEnd = resultSet.getTime("checkin_end").toLocalTime();
            SlotBuilder slot = day.slots.computeIfAbsent(slotId,
                    ignored -> new SlotBuilder(slotId, slotCode, checkinStart, checkinEnd));

            slot.tickets.add(TicketAvailabilityResponse.TicketAvailability.of(
                    resultSet.getString("stockID"), resultSet.getString("ticketTypeID"),
                    resultSet.getString("ticket_name"), resultSet.getBigDecimal("price"),
                    resultSet.getString("description"), resultSet.getInt("total_quantity"),
                    resultSet.getInt("sold_quantity"), resultSet.getInt("locked_quantity")));
        }, Date.valueOf(from), Date.valueOf(to));
        return days.values().stream().map(DayBuilder::build).toList();
    }

    private static final class DayBuilder {
        private final String openDayId;
        private final LocalDate visitDate;
        private final boolean holiday;
        private final Map<String, SlotBuilder> slots = new LinkedHashMap<>();

        private DayBuilder(String openDayId, LocalDate visitDate, boolean holiday) {
            this.openDayId = openDayId;
            this.visitDate = visitDate;
            this.holiday = holiday;
        }

        private TicketAvailabilityResponse build() {
            return new TicketAvailabilityResponse(openDayId, visitDate, holiday,
                    slots.values().stream().map(SlotBuilder::build).toList());
        }
    }

    private static final class SlotBuilder {
        private final String slotId;
        private final String slotCode;
        private final java.time.LocalTime checkinStart;
        private final java.time.LocalTime checkinEnd;
        private final List<TicketAvailabilityResponse.TicketAvailability> tickets = new ArrayList<>();

        private SlotBuilder(String slotId, String slotCode, java.time.LocalTime checkinStart,
                            java.time.LocalTime checkinEnd) {
            this.slotId = slotId;
            this.slotCode = slotCode;
            this.checkinStart = checkinStart;
            this.checkinEnd = checkinEnd;
        }

        private TicketAvailabilityResponse.SlotAvailability build() {
            return new TicketAvailabilityResponse.SlotAvailability(
                    slotId, slotCode, checkinStart, checkinEnd, List.copyOf(tickets));
        }
    }
}
