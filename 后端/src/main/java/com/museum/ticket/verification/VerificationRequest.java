package com.museum.ticket.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificationRequest(
        @NotBlank(message = "凭证码不能为空")
        @Size(min = 64, max = 64, message = "凭证码格式不正确")
        String voucherCode,
        String workerId
) {
}
