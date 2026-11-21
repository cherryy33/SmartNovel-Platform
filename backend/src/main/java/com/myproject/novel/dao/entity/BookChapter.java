package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说章节
 */
@Data
@TableName("book_chapter")
public class BookChapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;
    private Integer chapterNum;
    private String chapterName;
    private Integer wordCount;
    private Integer isVip;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
