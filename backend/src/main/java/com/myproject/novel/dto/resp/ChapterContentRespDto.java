package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * 章节内容响应（含正文）
 */
@Data
public class ChapterContentRespDto {

    private Long id;
    private Long bookId;
    private Integer chapterNum;
    private String chapterName;
    private Integer wordCount;
    private Integer isVip;
    private String content;

    /** 上一章 ID */
    private Long prevChapterId;

    /** 下一章 ID */
    private Long nextChapterId;

    /** 总章节数 */
    private Integer totalChapters;
}