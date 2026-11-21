package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 浏览历史 / 阅读进度
 */
@Data
@TableName("user_read_history")
public class UserReadHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long bookId;
    private Long lastChapterId;
    private LocalDateTime lastReadTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
