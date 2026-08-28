package com.museum.ticket.verification;

import com.museum.ticket.common.BusinessException;
import com.museum.ticket.common.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VerificationService {
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    private final JdbcTemplate jdbcTemplate;
    private final byte[] verificationKey;

    public VerificationService(JdbcTemplate jdbcTemplate,
                               @Value("${app.verification.key}") String verificationKey) {
        if (verificationKey.length() < 16) {
            throw new IllegalArgumentException("核销密钥长度不能少于 16 个字符");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.verificationKey = verificationKey.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public VerificationResponse verify(String providedKey, VerificationRequest request) {
        authenticate(providedKey);
        List<LockedVoucher> vouchers = jdbcTemplate.query("""
                SELECT v.voucherID, v.detailID, v.status, v.expired_at, o.status AS order_status
                FROM entry_voucher v
                JOIN orders_detail d ON d.detailID = v.detailID
                JOIN orders o ON o.ordersID = d.ordersID
                WHERE v.voucher_code = ? FOR UPDATE
                """, (resultSet, rowNumber) -> new LockedVoucher(resultSet.getString("voucherID"),
                resultSet.getString("detailID"), resultSet.getString("status"),
                resultSet.getTimestamp("expired_at").toLocalDateTime(),
                resultSet.getString("order_status")), request.voucherCode());
        if (vouchers.isEmpty()) {
            throw new BusinessException("电子凭证不存在");
        }
        LockedVoucher voucher = vouchers.getFirst();
        LocalDateTime now = LocalDateTime.now();
        if (!"有效".equals(voucher.status()) || !"已支付".equals(voucher.orderStatus())) {
            record(voucher.voucherId(), request.workerId(), "失败", "凭证状态不可核销");
            return new VerificationResponse(voucher.voucherId(), "失败", "凭证状态不可核销", now);
        }
        if (!voucher.expiredAt().isAfter(now)) {
            jdbcTemplate.update("UPDATE entry_voucher SET status = '已过期' WHERE voucherID = ?", voucher.voucherId());
            record(voucher.voucherId(), request.workerId(), "失败", "凭证已过期");
            return new VerificationResponse(voucher.voucherId(), "失败", "凭证已过期", now);
        }
        jdbcTemplate.update("UPDATE entry_voucher SET status = '已使用' WHERE voucherID = ?", voucher.voucherId());
        jdbcTemplate.update("UPDATE orders_detail SET verify_status = '已核验' WHERE detailID = ?", voucher.detailId());
        record(voucher.voucherId(), request.workerId(), "成功", "核销成功");
        return new VerificationResponse(voucher.voucherId(), "成功", "核销成功", now);
    }

    private void authenticate(String providedKey) {
        if (providedKey == null || !MessageDigest.isEqual(verificationKey,
                providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("核销密钥不正确");
        }
    }

    private void record(String voucherId, String workerId, String result, String remark) {
        jdbcTemplate.update("""
                INSERT INTO verification_record(verificationID, voucherID, workerID, result, remark)
                VALUES (?, ?, ?, ?, ?)
                """, generateId(), voucherId, workerId, result, remark);
    }

    private String generateId() {
        return "C" + LocalDateTime.now().format(ID_TIME_FORMAT)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private record LockedVoucher(String voucherId, String detailId, String status,
                                 LocalDateTime expiredAt, String orderStatus) {
    }
}
