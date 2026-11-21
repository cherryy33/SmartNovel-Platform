package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者信息 / 入驻审核
 */
@Data
@TableName("author_info")
public class AuthorInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String penName;
    private String intro;
    private Integer status;
    private String reviewComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
