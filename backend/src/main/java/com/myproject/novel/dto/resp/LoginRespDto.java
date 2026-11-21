package com.myproject.novel.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRespDto {

    private String token;
    private Long userId;
    private String username;
    private String nickName;
    private String userPhoto;
    private String role;
    private Integer isVip;
}