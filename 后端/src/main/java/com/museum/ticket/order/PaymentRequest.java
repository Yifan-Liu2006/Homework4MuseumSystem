package com.museum.ticket.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
        @NotBlank(message = "支付渠道不能为空")
        @Pattern(regexp = "^(微信支付|支付宝|其他)$", message = "支付渠道不正确")
        String channel
) {
}
