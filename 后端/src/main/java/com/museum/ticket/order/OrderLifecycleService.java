package com.museum.ticket.order;

import com.museum.ticket.auth.CurrentVisitor;
import com.museum.ticket.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderLifecycleService {
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderLifecycleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public OrderStatusResponse pay(String orderId, PaymentRequest request) {
        String visitorId = CurrentVisitor.requireVisitorId();
        LockedOrder order = lockOrder(orderId, visitorId);
        if ("已支付".equals(order.status())) {
            return response(orderId, "已支付", "已支付", "订单已支付，无需重复操作");
        }
        requirePending(order);
        if (!order.paymentDeadline().isAfter(LocalDateTime.now())) {
            expireLockedOrder(order);
            return response(orderId, "已过期", "未支付", "订单已超过支付期限，锁定库存已释放");
        }

        Map<String, Integer> quantities = lockOrderStocks(orderId);
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            int updated = jdbcTemplate.update("""
                    UPDATE ticket_stock
                    SET locked_quantity = locked_quantity - ?, sold_quantity = sold_quantity + ?
                    WHERE stockID = ? AND locked_quantity >= ?
                    """, entry.getValue(), entry.getValue(), entry.getKey(), entry.getValue());
            if (updated != 1) {
                throw new BusinessException("订单锁定库存异常，无法支付");
            }
        }

        LocalDateTime paidAt = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO payment_record(paymentID, ordersID, channel, amount, third_party_no, status, paid_at)
                VALUES (?, ?, ?, ?, ?, '成功', ?)
                """, generateId("Y"), orderId, request.channel(), order.price(),
                "SIM-" + generateId("T"), paidAt);
        jdbcTemplate.update("""
                UPDATE orders SET status = '已支付', pay_status = '已支付' WHERE ordersID = ?
                """, orderId);
        createVouchers(orderId);
        return response(orderId, "已支付", "已支付", "模拟支付成功");
    }

    @Transactional
    public OrderStatusResponse refund(String orderId) {
        LockedOrder order = lockOrder(orderId, CurrentVisitor.requireVisitorId());
        if ("已退款".equals(order.status())) {
            return response(orderId, "已退款", "已退款", "订单已退款，无需重复操作");
        }
        if (!"已支付".equals(order.status()) || !"已支付".equals(order.payStatus())) {
            throw new BusinessException("只有已支付订单可以退款");
        }
        LocalDate visitDate = jdbcTemplate.queryForObject(
                "SELECT visit_date FROM orders WHERE ordersID = ?", LocalDate.class, orderId);
        if (visitDate == null || visitDate.isBefore(LocalDate.now())) {
            throw new BusinessException("参观日期已过，不能退款");
        }
        Integer verifiedCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM orders_detail WHERE ordersID = ? AND verify_status = '已核验'
                """, Integer.class, orderId);
        if (verifiedCount != null && verifiedCount > 0) {
            throw new BusinessException("订单包含已核验凭证，不能退款");
        }

        Map<String, Integer> quantities = lockOrderStocks(orderId);
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            int updated = jdbcTemplate.update("""
                    UPDATE ticket_stock SET sold_quantity = sold_quantity - ?
                    WHERE stockID = ? AND sold_quantity >= ?
                    """, entry.getValue(), entry.getKey(), entry.getValue());
            if (updated != 1) {
                throw new BusinessException("已售库存异常，无法退款");
            }
        }
        jdbcTemplate.update("UPDATE orders SET status = '已退款', pay_status = '已退款' WHERE ordersID = ?", orderId);
        jdbcTemplate.update("UPDATE payment_record SET status = '已退款' WHERE ordersID = ?", orderId);
        jdbcTemplate.update("UPDATE orders_detail SET verify_status = '已作废' WHERE ordersID = ?", orderId);
        jdbcTemplate.update("""
                UPDATE entry_voucher v JOIN orders_detail d ON d.detailID = v.detailID
                SET v.status = '已作废' WHERE d.ordersID = ?
                """, orderId);
        return response(orderId, "已退款", "已退款", "退款成功，电子凭证已作废");
    }

    @Transactional
    public OrderStatusResponse cancel(String orderId) {
        LockedOrder order = lockOrder(orderId, CurrentVisitor.requireVisitorId());
        if ("已取消".equals(order.status())) {
            return response(orderId, "已取消", order.payStatus(), "订单已取消，无需重复操作");
        }
        requirePending(order);
        if (!order.paymentDeadline().isAfter(LocalDateTime.now())) {
            expireLockedOrder(order);
            return response(orderId, "已过期", "未支付", "订单已超过支付期限，锁定库存已释放");
        }
        releaseLockedStock(orderId);
        jdbcTemplate.update("UPDATE orders SET status = '已取消' WHERE ordersID = ?", orderId);
        return response(orderId, "已取消", "未支付", "订单取消成功");
    }

    @Transactional
    public boolean expire(String orderId) {
        List<LockedOrder> orders = jdbcTemplate.query("""
                SELECT ordersID, visitorID, price, status, pay_status, payment_deadline
                FROM orders WHERE ordersID = ? FOR UPDATE
                """, (resultSet, rowNumber) -> new LockedOrder(resultSet.getString("ordersID"),
                resultSet.getString("visitorID"), resultSet.getBigDecimal("price"),
                resultSet.getString("status"), resultSet.getString("pay_status"),
                resultSet.getTimestamp("payment_deadline").toLocalDateTime()), orderId);
        if (orders.isEmpty()) {
            return false;
        }
        LockedOrder order = orders.getFirst();
        if (!"待支付".equals(order.status()) || order.paymentDeadline().isAfter(LocalDateTime.now())) {
            return false;
        }
        expireLockedOrder(order);
        return true;
    }

    private LockedOrder lockOrder(String orderId, String visitorId) {
        List<LockedOrder> orders = jdbcTemplate.query("""
                SELECT ordersID, visitorID, price, status, pay_status, payment_deadline
                FROM orders WHERE ordersID = ? AND visitorID = ? FOR UPDATE
                """, (resultSet, rowNumber) -> new LockedOrder(resultSet.getString("ordersID"),
                resultSet.getString("visitorID"), resultSet.getBigDecimal("price"),
                resultSet.getString("status"), resultSet.getString("pay_status"),
                resultSet.getTimestamp("payment_deadline").toLocalDateTime()), orderId, visitorId);
        if (orders.isEmpty()) {
            throw new BusinessException("订单不存在或无权访问");
        }
        return orders.getFirst();
    }

    private void requirePending(LockedOrder order) {
        if (!"待支付".equals(order.status()) || !"未支付".equals(order.payStatus())) {
            throw new BusinessException("当前订单状态不允许此操作");
        }
    }

    private Map<String, Integer> lockOrderStocks(String orderId) {
        List<StockQuantity> rows = jdbcTemplate.query("""
                SELECT st.stockID, COUNT(*) AS quantity
                FROM orders_detail d JOIN ticket_stock st ON st.stockID = d.stockID
                WHERE d.ordersID = ? GROUP BY st.stockID ORDER BY st.stockID FOR UPDATE
                """, (resultSet, rowNumber) -> new StockQuantity(
                resultSet.getString("stockID"), resultSet.getInt("quantity")), orderId);
        Map<String, Integer> quantities = new LinkedHashMap<>();
        rows.forEach(row -> quantities.put(row.stockId(), row.quantity()));
        if (quantities.isEmpty()) {
            throw new BusinessException("订单没有有效明细");
        }
        return quantities;
    }

    private void releaseLockedStock(String orderId) {
        Map<String, Integer> quantities = lockOrderStocks(orderId);
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            int updated = jdbcTemplate.update("""
                    UPDATE ticket_stock SET locked_quantity = locked_quantity - ?
                    WHERE stockID = ? AND locked_quantity >= ?
                    """, entry.getValue(), entry.getKey(), entry.getValue());
            if (updated != 1) {
                throw new BusinessException("订单锁定库存异常，无法释放");
            }
        }
    }

    private void expireLockedOrder(LockedOrder order) {
        releaseLockedStock(order.orderId());
        jdbcTemplate.update("UPDATE orders SET status = '已过期' WHERE ordersID = ?", order.orderId());
    }

    private void createVouchers(String orderId) {
        LocalDate visitDate = jdbcTemplate.queryForObject(
                "SELECT visit_date FROM orders WHERE ordersID = ?", LocalDate.class, orderId);
        if (visitDate == null) {
            throw new BusinessException("订单参观日期不存在");
        }
        List<String> detailIds = jdbcTemplate.queryForList(
                "SELECT detailID FROM orders_detail WHERE ordersID = ? ORDER BY detailID", String.class, orderId);
        LocalDateTime expiredAt = LocalDateTime.of(visitDate.plusDays(1), LocalTime.MIDNIGHT);
        for (String detailId : detailIds) {
            jdbcTemplate.update("""
                    INSERT INTO entry_voucher(voucherID, detailID, voucher_code, expired_at, status)
                    VALUES (?, ?, ?, ?, '有效')
                    """, generateId("V"), detailId, generateVoucherCode(detailId), expiredAt);
        }
    }

    private String generateVoucherCode(String detailId) {
        try {
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(detailId.getBytes(StandardCharsets.UTF_8));
            digest.update(randomBytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成电子凭证", exception);
        }
    }

    private OrderStatusResponse response(String orderId, String status, String payStatus, String message) {
        return new OrderStatusResponse(orderId, status, payStatus, message, LocalDateTime.now());
    }

    private String generateId(String prefix) {
        return prefix + LocalDateTime.now().format(ID_TIME_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private record LockedOrder(String orderId, String visitorId, BigDecimal price, String status,
                               String payStatus, LocalDateTime paymentDeadline) {
    }

    private record StockQuantity(String stockId, int quantity) {
    }
}
