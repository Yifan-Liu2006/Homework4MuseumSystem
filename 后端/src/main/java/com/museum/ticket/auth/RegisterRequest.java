package com.museum.ticket.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为 8 至 72 位")
        String password
) {
}
