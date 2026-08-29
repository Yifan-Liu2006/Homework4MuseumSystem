package com.museum.ticket.admin;

import com.museum.ticket.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/operations")
public class AdminOperationsController {
    private final JdbcTemplate jdbc;
    public AdminOperationsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/orders")
    public List<Map<String,Object>> orders(@RequestParam(required=false) String status,
                                           @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(defaultValue="50") int limit,
                                           @RequestParam(defaultValue="0") int offset) {
        validatePage(limit, offset); StringBuilder sql=new StringBuilder("SELECT o.*,v.mobile FROM orders o JOIN visitor v ON v.visitorID=o.visitorID WHERE 1=1 ");
        java.util.ArrayList<Object> args=new java.util.ArrayList<>();
        if(status!=null){sql.append("AND o.status=? ");args.add(status);} if(from!=null){sql.append("AND o.visit_date>=? ");args.add(from);} if(to!=null){sql.append("AND o.visit_date<=? ");args.add(to);}
        sql.append("ORDER BY o.order_date DESC LIMIT ? OFFSET ?");args.add(limit);args.add(offset); log("查询订单", "orders");
        return jdbc.queryForList(sql.toString(),args.toArray());
    }

    @GetMapping("/payments")
    public List<Map<String,Object>> payments(@RequestParam(required=false) String status,
                                             @RequestParam(defaultValue="50") int limit,
                                             @RequestParam(defaultValue="0") int offset) {
        validatePage(limit,offset); String sql="SELECT p.*,o.visitorID,o.visit_date FROM payment_record p JOIN orders o ON o.ordersID=p.ordersID "+(status==null?"":"WHERE p.status=? ")+"ORDER BY p.paid_at DESC LIMIT ? OFFSET ?";
        log("查询支付记录","payment_record"); return status==null?jdbc.queryForList(sql,limit,offset):jdbc.queryForList(sql,status,limit,offset);
    }

    @GetMapping("/verifications")
    public List<Map<String,Object>> verifications(@RequestParam(required=false) String result,
                                                  @RequestParam(defaultValue="50") int limit,
                                                  @RequestParam(defaultValue="0") int offset) {
        validatePage(limit,offset); String sql="SELECT r.*,v.voucher_code,d.ordersID,o.visit_date FROM verification_record r JOIN entry_voucher v ON v.voucherID=r.voucherID JOIN orders_detail d ON d.detailID=v.detailID JOIN orders o ON o.ordersID=d.ordersID "+(result==null?"":"WHERE r.result=? ")+"ORDER BY r.verified_at DESC LIMIT ? OFFSET ?";
        log("查询核销记录","verification_record"); return result==null?jdbc.queryForList(sql,limit,offset):jdbc.queryForList(sql,result,limit,offset);
    }

    @GetMapping("/statistics")
    public Map<String,Object> statistics(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        if(to.isBefore(from)||to.isAfter(from.plusDays(366))) throw new BusinessException("统计日期范围不正确");
        Map<String,Object> summary=jdbc.queryForMap("""
                SELECT COUNT(*) order_count,
                       COALESCE(SUM(CASE WHEN status IN ('已支付','已退款') THEN price ELSE 0 END),0) total_amount,
                       COALESCE(SUM(CASE WHEN status='已支付' THEN price ELSE 0 END),0) paid_amount,
                       COALESCE(SUM(CASE WHEN status='已支付' THEN 1 ELSE 0 END),0) paid_order_count
                FROM orders WHERE visit_date BETWEEN ? AND ?
                """,from,to);
        Map<String,Object> details=jdbc.queryForMap("""
                SELECT COUNT(*) ticket_count,
                       COALESCE(SUM(CASE WHEN d.verify_status='已核验' THEN 1 ELSE 0 END),0) verified_count
                FROM orders_detail d JOIN orders o ON o.ordersID=d.ordersID
                WHERE o.visit_date BETWEEN ? AND ? AND o.status='已支付'
                """,from,to);
        java.util.Map<String,Object> response=new java.util.LinkedHashMap<>(summary); response.putAll(details); log("查询运营统计",from+"~"+to); return response;
    }

    private void validatePage(int limit,int offset){if(limit<1||limit>200||offset<0)throw new BusinessException("分页参数不正确");}
    private void log(String type,String object){try{AdminCurrent.AdminPrincipal p=AdminCurrent.require();jdbc.update("INSERT INTO operation_log(logID,workerID,operation_type,operation_object,result) VALUES(?,?,?,?,?)",AdminIdGenerator.generate("L"),p.workerId(),type,object,"成功");}catch(Exception ignored){}}
}
