package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
@Data
public class UserInfoRespDto {

    private Long id;
    private String username;
    /** 角色：READER / AUTHOR / ADMIN */
    private String role;
    private String nickName;
    private String userPhoto;
    private Integer userSex;
    private String email;
    private String phone;
    private Integer level;
    private Integer isVip;
    private Integer status;
    private LocalDateTime createTime;
}