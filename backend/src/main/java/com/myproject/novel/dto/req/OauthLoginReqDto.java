package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方模拟登录请求
 */
@Data
public class OauthLoginReqDto {

    @NotBlank(message = "第三方类型不能为空")
    private String oauthType;

    @NotBlank(message = "第三方标识不能为空")
    private String oauthId;

    /** 第三方昵称（模拟时自动生成） */
    private String nickName;
}
