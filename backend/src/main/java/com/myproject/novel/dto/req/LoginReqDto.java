package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginReqDto {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录身份：READER / AUTHOR / ADMIN（未传默认 READER） */
    private String role;

    /** 验证码 key（从 /captcha 接口获取） */
    private String captchaKey;

    /** 验证码答案 */
    private String captchaCode;
}
