package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 书评 / 章节评论
 */
@Data
@TableName("book_comment")
public class BookComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;
    private Long chapterId;
    private Long userId;
    private String content;
    private Integer replyCount;
    private Integer auditStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
