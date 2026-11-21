package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方登录绑定（模拟 OAuth）
 */
@Data
@TableName("user_oauth")
public class UserOauth {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String oauthType;
    private String oauthId;
    private LocalDateTime createTime;
}
