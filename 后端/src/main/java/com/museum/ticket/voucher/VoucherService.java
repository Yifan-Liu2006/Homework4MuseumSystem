package com.museum.ticket.voucher;

import com.museum.ticket.auth.CurrentVisitor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoucherService {
    private final JdbcTemplate jdbcTemplate;

    public VoucherService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<VoucherResponse> list() {
        return jdbcTemplate.query("""
                SELECT v.voucherID, v.voucher_code, v.status, v.generated_at, v.expired_at,
                       o.ordersID, o.visit_date, p.name AS person_name, p.id_masked,
                       tt.name AS ticket_name, vs.slot_code, vs.checkin_start, vs.checkin_end
                FROM entry_voucher v
                JOIN orders_detail d ON d.detailID = v.detailID
                JOIN orders o ON o.ordersID = d.ordersID
                JOIN real_person p ON p.personID = d.personID
                JOIN ticket_stock st ON st.stockID = d.stockID
                JOIN ticket_type tt ON tt.ticketTypeID = st.ticketTypeID
                JOIN visit_slot vs ON vs.slotID = st.slotID
                WHERE o.visitorID = ?
                ORDER BY o.visit_date DESC, vs.checkin_start, p.name
                """, (resultSet, rowNumber) -> new VoucherResponse(
                resultSet.getString("voucherID"), resultSet.getString("voucher_code"),
                resultSet.getString("status"), resultSet.getTimestamp("generated_at").toLocalDateTime(),
                resultSet.getTimestamp("expired_at").toLocalDateTime(), resultSet.getString("ordersID"),
                resultSet.getDate("visit_date").toLocalDate(), resultSet.getString("person_name"),
                resultSet.getString("id_masked"), resultSet.getString("ticket_name"),
                resultSet.getString("slot_code"), resultSet.getTime("checkin_start").toLocalTime(),
                resultSet.getTime("checkin_end").toLocalTime()), CurrentVisitor.requireVisitorId());
    }
}
