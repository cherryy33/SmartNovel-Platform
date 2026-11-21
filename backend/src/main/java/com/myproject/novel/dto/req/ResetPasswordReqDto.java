package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码重置请求
 */
@Data
public class ResetPasswordReqDto {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度需在 6-50 之间")
    private String newPassword;

    /** 验证码 */
    private String code;
}
