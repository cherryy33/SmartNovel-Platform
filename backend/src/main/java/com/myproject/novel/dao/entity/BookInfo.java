package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说信息
 */
@Data
@TableName("book_info")
public class BookInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer workDirection;
    private Long categoryId;
    private String categoryName;
    private String picUrl;
    private String bookName;
    private Long authorId;
    private String authorName;
    private String bookDesc;
    private Integer score;
    private Integer bookStatus;
    private Long visitCount;
    private Integer wordCount;
    private Integer commentCount;
    private Long lastChapterId;
    private String lastChapterName;
    private LocalDateTime lastChapterUpdateTime;
    private Integer isVip;
    private Integer auditStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
