package com.myproject.novel.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户主体（写入 JWT / 存于 SecurityContext）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户 ID：读者/作者 = user_info.id，管理员 = sys_user.id */
    private Long userId;

    /** 登录名 */
    private String username;

    /** 角色：USER / AUTHOR / ADMIN */
    private String role;

    /** 作者 ID（角色为 AUTHOR 时有效） */
    private Long authorId;

    /** 是否 VIP（0-否 1-是） */
    private Integer isVip;
}
