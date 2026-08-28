package com.museum.ticket.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "订单至少需要一张票")
        @Size(max = 10, message = "单次最多预订 10 张票")
        List<@Valid OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotBlank(message = "实名人员编号不能为空") String personId,
            @NotBlank(message = "库存编号不能为空") String stockId
    ) {
    }
}
