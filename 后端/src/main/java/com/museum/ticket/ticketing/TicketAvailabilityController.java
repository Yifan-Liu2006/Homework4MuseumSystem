package com.museum.ticket.ticketing;

import com.museum.ticket.common.BusinessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ticketing")
public class TicketAvailabilityController {
    private final TicketAvailabilityService ticketAvailabilityService;

    public TicketAvailabilityController(TicketAvailabilityService ticketAvailabilityService) {
        this.ticketAvailabilityService = ticketAvailabilityService;
    }

    @GetMapping("/availability")
    public List<TicketAvailabilityResponse> availability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate actualFrom = from == null ? LocalDate.now() : from;
        LocalDate actualTo = to == null ? actualFrom.plusDays(30) : to;
        if (actualFrom.isBefore(LocalDate.now())) {
            throw new BusinessException("开始日期不能早于今天");
        }
        if (actualTo.isBefore(actualFrom)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        if (actualTo.isAfter(actualFrom.plusDays(90))) {
            throw new BusinessException("单次最多查询 90 天");
        }
        return ticketAvailabilityService.findAvailability(actualFrom, actualTo);
    }
}
