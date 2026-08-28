package com.museum.ticket.person;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RealPersonRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名不能超过 50 个字符")
        String name,
        @NotBlank(message = "证件类型不能为空")
        @Pattern(regexp = "^(身份证|港澳台通行证|护照)$", message = "证件类型不正确")
        String idType,
        @NotBlank(message = "证件号码不能为空")
        @Size(min = 5, max = 40, message = "证件号码长度必须为 5 至 40 位")
        String idNumber,
        @NotNull(message = "是否本人不能为空")
        Boolean isSelf
) {
}
