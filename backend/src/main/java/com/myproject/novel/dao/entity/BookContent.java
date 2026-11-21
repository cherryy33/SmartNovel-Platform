package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节内容
 */
@Data
@TableName("book_content")
public class BookContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chapterId;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
