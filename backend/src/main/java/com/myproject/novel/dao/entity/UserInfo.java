package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 普通用户（读者/作者）
 */
@Data
@TableName("user_info")
public class UserInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;

    /** 角色：READER / AUTHOR */
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
    private LocalDateTime updateTime;
}
