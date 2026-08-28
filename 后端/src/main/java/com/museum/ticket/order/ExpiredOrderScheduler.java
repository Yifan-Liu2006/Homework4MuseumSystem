package com.museum.ticket.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExpiredOrderScheduler {
    private final JdbcTemplate jdbcTemplate;
    private final OrderLifecycleService orderLifecycleService;

    public ExpiredOrderScheduler(JdbcTemplate jdbcTemplate, OrderLifecycleService orderLifecycleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderLifecycleService = orderLifecycleService;
    }

    @Scheduled(fixedDelayString = "${app.order.expiration-scan-milliseconds:60000}")
    public void expireOrders() {
        List<String> orderIds = jdbcTemplate.queryForList("""
                SELECT ordersID FROM orders
                WHERE status = '待支付' AND payment_deadline <= NOW()
                ORDER BY payment_deadline LIMIT 100
                """, String.class);
        orderIds.forEach(orderLifecycleService::expire);
    }
}
