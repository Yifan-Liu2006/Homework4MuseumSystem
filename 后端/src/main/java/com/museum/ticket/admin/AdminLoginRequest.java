package com.museum.ticket.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "管理员账号不能为空") String account,
        @NotBlank(message = "管理员密码不能为空") String password
) {
}
