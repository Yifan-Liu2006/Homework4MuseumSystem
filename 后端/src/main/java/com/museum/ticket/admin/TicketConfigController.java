package com.museum.ticket.admin;

import com.museum.ticket.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ticket-config")
public class TicketConfigController {
    private final JdbcTemplate jdbc;
    public TicketConfigController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/open-days")
    public List<Map<String,Object>> openDays() { return jdbc.queryForList("SELECT * FROM open_day ORDER BY visit_date DESC"); }

    @PostMapping("/open-days")
    public Map<String,Object> createOpenDay(@Valid @RequestBody OpenDayRequest r) {
        String id = AdminIdGenerator.generate("D");
        jdbc.update("INSERT INTO open_day(openDayID,visit_date,is_closed,is_holiday,release_time,status) VALUES(?,?,?,?,?,'未开票')",
                id,r.visitDate(),r.closed(),r.holiday(),r.releaseTime());
        log("新增开放日",id); return jdbc.queryForMap("SELECT * FROM open_day WHERE openDayID=?",id);
    }

    @PutMapping("/open-days/{id}")
    public Map<String,Object> updateOpenDay(@PathVariable String id,@Valid @RequestBody OpenDayRequest r) {
        int n=jdbc.update("UPDATE open_day SET visit_date=?,is_closed=?,is_holiday=?,release_time=?,status=? WHERE openDayID=?",
                r.visitDate(),r.closed(),r.holiday(),r.releaseTime(),r.status(),id);
        if(n==0) throw new BusinessException("开放日不存在"); log("修改开放日",id); return jdbc.queryForMap("SELECT * FROM open_day WHERE openDayID=?",id);
    }

    @GetMapping("/ticket-types")
    public List<Map<String,Object>> ticketTypes(){return jdbc.queryForList("SELECT * FROM ticket_type ORDER BY name");}
    @PostMapping("/ticket-types")
    public Map<String,Object> createType(@Valid @RequestBody TicketTypeRequest r){String id=AdminIdGenerator.generate("T"); jdbc.update("INSERT INTO ticket_type(ticketTypeID,name,price,description,status) VALUES(?,?,?,?,?)",id,r.name(),r.price(),r.description(),r.status()); log("新增票种",id); return jdbc.queryForMap("SELECT * FROM ticket_type WHERE ticketTypeID=?",id);}
    @PutMapping("/ticket-types/{id}")
    public Map<String,Object> updateType(@PathVariable String id,@Valid @RequestBody TicketTypeRequest r){if(jdbc.update("UPDATE ticket_type SET name=?,price=?,description=?,status=? WHERE ticketTypeID=?",r.name(),r.price(),r.description(),r.status(),id)==0) throw new BusinessException("票种不存在"); log("修改票种",id); return jdbc.queryForMap("SELECT * FROM ticket_type WHERE ticketTypeID=?",id);}

    @GetMapping("/slots") public List<Map<String,Object>> slots(){return jdbc.queryForList("SELECT * FROM visit_slot ORDER BY openDayID,checkin_start");}
    @PostMapping("/slots") public Map<String,Object> createSlot(@Valid @RequestBody SlotRequest r){String id=AdminIdGenerator.generate("S"); jdbc.update("INSERT INTO visit_slot(slotID,openDayID,slot_code,checkin_start,checkin_end,status) VALUES(?,?,?,?,?,?)",id,r.openDayId(),r.slotCode(),r.checkinStart(),r.checkinEnd(),r.status()); log("新增场次",id); return jdbc.queryForMap("SELECT * FROM visit_slot WHERE slotID=?",id);}
    @PutMapping("/slots/{id}") public Map<String,Object> updateSlot(@PathVariable String id,@Valid @RequestBody SlotRequest r){if(jdbc.update("UPDATE visit_slot SET slot_code=?,checkin_start=?,checkin_end=?,status=? WHERE slotID=?",r.slotCode(),r.checkinStart(),r.checkinEnd(),r.status(),id)==0) throw new BusinessException("场次不存在"); log("修改场次",id); return jdbc.queryForMap("SELECT * FROM visit_slot WHERE slotID=?",id);}

    @GetMapping("/stocks") public List<Map<String,Object>> stocks(){return jdbc.queryForList("SELECT * FROM ticket_stock ORDER BY slotID,ticketTypeID");}
    @PostMapping("/stocks") public Map<String,Object> createStock(@Valid @RequestBody StockRequest r){String id=AdminIdGenerator.generate("K"); jdbc.update("INSERT INTO ticket_stock(stockID,slotID,ticketTypeID,total_quantity) VALUES(?,?,?,?)",id,r.slotId(),r.ticketTypeId(),r.totalQuantity()); log("新增库存",id); return jdbc.queryForMap("SELECT * FROM ticket_stock WHERE stockID=?",id);}
    @PutMapping("/stocks/{id}") public Map<String,Object> updateStock(@PathVariable String id,@Valid @RequestBody StockRequest r){int n=jdbc.update("UPDATE ticket_stock SET total_quantity=? WHERE stockID=? AND total_quantity>=sold_quantity+locked_quantity",r.totalQuantity(),id); if(n==0) throw new BusinessException("库存不存在或新库存小于已占用数量"); log("调整库存",id); return jdbc.queryForMap("SELECT * FROM ticket_stock WHERE stockID=?",id);}

    private void log(String type,String object){try{AdminCurrent.AdminPrincipal p=AdminCurrent.require(); jdbc.update("INSERT INTO operation_log(logID,workerID,operation_type,operation_object,result) VALUES(?,?,?,?,?)",AdminIdGenerator.generate("L"),p.workerId(),type,object,"成功");}catch(Exception ignored){}}
    public record OpenDayRequest(@NotNull LocalDate visitDate,boolean closed,boolean holiday,@NotNull LocalDateTime releaseTime,String status) { public OpenDayRequest { if(status==null) status="未开票"; } }
    public record TicketTypeRequest(@NotBlank String name,@NotNull @DecimalMin("0") BigDecimal price,String description,String status) { public TicketTypeRequest { if(status==null) status="上架"; } }
    public record SlotRequest(@NotBlank String openDayId,@NotBlank String slotCode,@NotNull LocalTime checkinStart,@NotNull LocalTime checkinEnd,String status) { public SlotRequest { if(status==null) status="启用"; } }
    public record StockRequest(@NotBlank String slotId,@NotBlank String ticketTypeId,@NotNull @Min(0) Integer totalQuantity) {}
}
