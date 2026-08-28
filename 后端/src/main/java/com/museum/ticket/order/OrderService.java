package com.museum.ticket.order;

import com.museum.ticket.auth.CurrentVisitor;
import com.museum.ticket.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    private final JdbcTemplate jdbcTemplate;

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        String visitorId = CurrentVisitor.requireVisitorId();
        validateRequestDuplicates(request.items());
        validatePeopleOwnership(visitorId, request.items());

        Map<String, StockSnapshot> stocks = lockStocks(request.items());
        LocalDate visitDate = validateAndGetVisitDate(request.items(), stocks);
        validateRequestSlotDuplicates(request.items(), stocks);
        validateDuplicateReservations(request.items(), stocks);
        validateRequestedQuantities(request.items(), stocks);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(15);
        String orderId = generateId("O");
        BigDecimal totalPrice = request.items().stream()
                .map(item -> stocks.get(item.stockId()).price())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        jdbcTemplate.update("""
                INSERT INTO orders(ordersID, visitorID, visit_date, price, order_date, status, pay_status, payment_deadline)
                VALUES (?, ?, ?, ?, ?, '待支付', '未支付', ?)
                """, orderId, visitorId, visitDate, totalPrice, now, deadline);

        for (CreateOrderRequest.OrderItemRequest item : request.items()) {
            StockSnapshot stock = stocks.get(item.stockId());
            jdbcTemplate.update("""
                    INSERT INTO orders_detail(detailID, ordersID, personID, stockID, price, verify_status)
                    VALUES (?, ?, ?, ?, ?, '未核验')
                    """, generateId("D"), orderId, item.personId(), item.stockId(), stock.price());
            jdbcTemplate.update("""
                    UPDATE ticket_stock SET locked_quantity = locked_quantity + 1 WHERE stockID = ?
                    """, item.stockId());
        }
        return requireOwnedOrder(orderId, visitorId);
    }

    public List<OrderResponse> list() {
        String visitorId = CurrentVisitor.requireVisitorId();
        return queryOrders("WHERE o.visitorID = ? ORDER BY o.order_date DESC", visitorId);
    }

    public OrderResponse get(String orderId) {
        return requireOwnedOrder(orderId, CurrentVisitor.requireVisitorId());
    }

    private void validateRequestDuplicates(List<CreateOrderRequest.OrderItemRequest> items) {
        long distinct = items.stream().map(item -> item.personId() + "\u0000" + item.stockId()).distinct().count();
        if (distinct != items.size()) {
            throw new BusinessException("订单中存在重复的实名人员和票种");
        }
    }

    private void validatePeopleOwnership(String visitorId, List<CreateOrderRequest.OrderItemRequest> items) {
        List<String> personIds = items.stream().map(CreateOrderRequest.OrderItemRequest::personId).distinct().toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(personIds.size(), "?"));
        List<Object> arguments = new ArrayList<>();
        arguments.add(visitorId);
        arguments.addAll(personIds);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM real_person WHERE visitorID = ? AND personID IN (" + placeholders + ")",
                Integer.class, arguments.toArray());
        if (count == null || count != personIds.size()) {
            throw new BusinessException("实名人员不存在或不属于当前游客");
        }
    }

    private Map<String, StockSnapshot> lockStocks(List<CreateOrderRequest.OrderItemRequest> items) {
        Map<String, StockSnapshot> stocks = new HashMap<>();
        items.stream().map(CreateOrderRequest.OrderItemRequest::stockId).distinct().sorted().forEach(stockId -> {
            List<StockSnapshot> result = jdbcTemplate.query("""
                    SELECT st.stockID, st.total_quantity, st.sold_quantity, st.locked_quantity,
                           tt.price, tt.status AS ticket_status, vs.slotID, vs.status AS slot_status,
                           od.visit_date, od.status AS day_status, od.is_closed, od.release_time
                    FROM ticket_stock st
                    JOIN ticket_type tt ON tt.ticketTypeID = st.ticketTypeID
                    JOIN visit_slot vs ON vs.slotID = st.slotID
                    JOIN open_day od ON od.openDayID = vs.openDayID
                    WHERE st.stockID = ? FOR UPDATE
                    """, (resultSet, rowNumber) -> mapStock(resultSet), stockId);
            if (result.isEmpty()) {
                throw new BusinessException("所选票种库存不存在");
            }
            StockSnapshot stock = result.getFirst();
            if (!"上架".equals(stock.ticketStatus()) || !"启用".equals(stock.slotStatus())
                    || !"已开票".equals(stock.dayStatus()) || stock.closed()
                    || stock.releaseTime().isAfter(LocalDateTime.now())
                    || stock.visitDate().isBefore(LocalDate.now())) {
                throw new BusinessException("所选场次当前不可预约");
            }
            stocks.put(stockId, stock);
        });
        return stocks;
    }

    private LocalDate validateAndGetVisitDate(List<CreateOrderRequest.OrderItemRequest> items,
                                               Map<String, StockSnapshot> stocks) {
        List<LocalDate> dates = items.stream().map(item -> stocks.get(item.stockId()).visitDate()).distinct().toList();
        if (dates.size() != 1) {
            throw new BusinessException("同一订单只能预订同一天的门票");
        }
        return dates.getFirst();
    }

    private void validateDuplicateReservations(List<CreateOrderRequest.OrderItemRequest> items,
                                                Map<String, StockSnapshot> stocks) {
        for (CreateOrderRequest.OrderItemRequest item : items) {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM orders_detail d
                    JOIN orders o ON o.ordersID = d.ordersID
                    JOIN ticket_stock st ON st.stockID = d.stockID
                    WHERE d.personID = ? AND st.slotID = ?
                      AND o.status NOT IN ('已取消', '已退款', '已过期')
                    """, Integer.class, item.personId(), stocks.get(item.stockId()).slotId());
            if (count != null && count > 0) {
                throw new BusinessException("实名人员已预约所选场次");
            }
        }
    }

    private void validateRequestSlotDuplicates(List<CreateOrderRequest.OrderItemRequest> items,
                                               Map<String, StockSnapshot> stocks) {
        long distinct = items.stream()
                .map(item -> item.personId() + "\u0000" + stocks.get(item.stockId()).slotId())
                .distinct().count();
        if (distinct != items.size()) {
            throw new BusinessException("同一实名人员不能重复预订同一场次");
        }
    }

    private void validateRequestedQuantities(List<CreateOrderRequest.OrderItemRequest> items,
                                             Map<String, StockSnapshot> stocks) {
        Map<String, Long> quantities = items.stream().collect(java.util.stream.Collectors.groupingBy(
                CreateOrderRequest.OrderItemRequest::stockId, java.util.stream.Collectors.counting()));
        quantities.forEach((stockId, quantity) -> {
            StockSnapshot stock = stocks.get(stockId);
            long available = (long) stock.totalQuantity() - stock.soldQuantity() - stock.lockedQuantity();
            if (available < quantity) {
                throw new BusinessException("所选票种库存不足");
            }
        });
    }

    private OrderResponse requireOwnedOrder(String orderId, String visitorId) {
        List<OrderResponse> orders = queryOrders("WHERE o.ordersID = ? AND o.visitorID = ?", orderId, visitorId);
        if (orders.isEmpty()) {
            throw new BusinessException("订单不存在或无权访问");
        }
        return orders.getFirst();
    }

    private List<OrderResponse> queryOrders(String whereClause, Object... arguments) {
        String sql = """
                SELECT o.ordersID, o.visit_date, o.price AS order_price, o.order_date, o.status,
                       o.pay_status, o.payment_deadline, d.detailID, d.personID, p.name AS person_name,
                       p.id_masked, d.stockID, tt.name AS ticket_name, vs.slot_code,
                       vs.checkin_start, vs.checkin_end, d.price AS detail_price, d.verify_status
                FROM orders o
                LEFT JOIN orders_detail d ON d.ordersID = o.ordersID
                LEFT JOIN real_person p ON p.personID = d.personID
                LEFT JOIN ticket_stock st ON st.stockID = d.stockID
                LEFT JOIN ticket_type tt ON tt.ticketTypeID = st.ticketTypeID
                LEFT JOIN visit_slot vs ON vs.slotID = st.slotID
                """ + whereClause;
        Map<String, OrderBuilder> orders = new LinkedHashMap<>();
        jdbcTemplate.query(sql, resultSet -> {
            String orderId = resultSet.getString("ordersID");
            OrderBuilder order = orders.computeIfAbsent(orderId, ignored -> mapOrderBuilder(resultSet));
            if (resultSet.getString("detailID") != null) {
                order.details.add(mapDetail(resultSet));
            }
        }, arguments);
        return orders.values().stream().map(OrderBuilder::build).toList();
    }

    private StockSnapshot mapStock(ResultSet resultSet) throws SQLException {
        return new StockSnapshot(resultSet.getString("stockID"), resultSet.getString("slotID"),
                resultSet.getBigDecimal("price"), resultSet.getInt("total_quantity"),
                resultSet.getInt("sold_quantity"), resultSet.getInt("locked_quantity"),
                resultSet.getString("ticket_status"), resultSet.getString("slot_status"),
                resultSet.getDate("visit_date").toLocalDate(), resultSet.getString("day_status"),
                resultSet.getBoolean("is_closed"), resultSet.getTimestamp("release_time").toLocalDateTime());
    }

    private OrderBuilder mapOrderBuilder(ResultSet resultSet) {
        try {
            return new OrderBuilder(resultSet.getString("ordersID"), resultSet.getDate("visit_date").toLocalDate(),
                    resultSet.getBigDecimal("order_price"), resultSet.getTimestamp("order_date").toLocalDateTime(),
                    resultSet.getString("status"), resultSet.getString("pay_status"),
                    resultSet.getTimestamp("payment_deadline").toLocalDateTime());
        } catch (SQLException exception) {
            throw new IllegalStateException("读取订单失败", exception);
        }
    }

    private OrderResponse.OrderDetailResponse mapDetail(ResultSet resultSet) throws SQLException {
        return new OrderResponse.OrderDetailResponse(resultSet.getString("detailID"),
                resultSet.getString("personID"), resultSet.getString("person_name"),
                resultSet.getString("id_masked"), resultSet.getString("stockID"),
                resultSet.getString("ticket_name"), resultSet.getString("slot_code"),
                toLocalTime(resultSet.getTime("checkin_start")), toLocalTime(resultSet.getTime("checkin_end")),
                resultSet.getBigDecimal("detail_price"), resultSet.getString("verify_status"));
    }

    private LocalTime toLocalTime(java.sql.Time value) {
        return value == null ? null : value.toLocalTime();
    }

    private String generateId(String prefix) {
        return prefix + LocalDateTime.now().format(ID_TIME_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private record StockSnapshot(String stockId, String slotId, BigDecimal price, int totalQuantity,
                                 int soldQuantity, int lockedQuantity, String ticketStatus, String slotStatus,
                                 LocalDate visitDate, String dayStatus, boolean closed,
                                 LocalDateTime releaseTime) {
    }

    private static final class OrderBuilder {
        private final String orderId;
        private final LocalDate visitDate;
        private final BigDecimal price;
        private final LocalDateTime orderDate;
        private final String status;
        private final String payStatus;
        private final LocalDateTime paymentDeadline;
        private final List<OrderResponse.OrderDetailResponse> details = new ArrayList<>();

        private OrderBuilder(String orderId, LocalDate visitDate, BigDecimal price, LocalDateTime orderDate,
                             String status, String payStatus, LocalDateTime paymentDeadline) {
            this.orderId = orderId;
            this.visitDate = visitDate;
            this.price = price;
            this.orderDate = orderDate;
            this.status = status;
            this.payStatus = payStatus;
            this.paymentDeadline = paymentDeadline;
        }

        private OrderResponse build() {
            return new OrderResponse(orderId, visitDate, price, orderDate, status, payStatus, paymentDeadline,
                    List.copyOf(details));
        }
    }
}
