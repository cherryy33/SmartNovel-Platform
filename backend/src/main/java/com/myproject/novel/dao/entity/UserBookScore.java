package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说评分（1-5 星）
 */
@Data
@TableName("user_book_score")
public class UserBookScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long bookId;
    private Integer score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
