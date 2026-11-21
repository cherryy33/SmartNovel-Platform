package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小说信息响应（列表/详情共用）
 */
@Data
public class BookInfoRespDto {

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

    /** 标签列表 */
    private List<String> tags;

    /** 当前用户是否已收藏 */
    private Boolean isFavorited;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 当前用户评分 */
    private Integer myScore;
}