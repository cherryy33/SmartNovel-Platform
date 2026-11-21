package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterReqDto {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度需在 2-50 之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度需在 6-50 之间")
    private String password;

    /** 注册身份：READER / AUTHOR / ADMIN，未传默认 READER */
    private String role;

    /** 笔名（以 AUTHOR 身份注册时可用，未填则使用用户名） */
    private String penName;

    /** 验证码 key */
    private String captchaKey;

    /** 验证码答案 */
    private String captchaCode;
}
