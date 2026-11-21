package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阅读历史响应
 */
@Data
public class ReadHistoryRespDto {

    private Long id;
    private Long userId;
    private Long bookId;
    private String bookName;
    private String picUrl;
    private String authorName;
    private Long lastChapterId;
    private String lastChapterName;
    private Integer lastChapterNum;
    private LocalDateTime lastReadTime;
}