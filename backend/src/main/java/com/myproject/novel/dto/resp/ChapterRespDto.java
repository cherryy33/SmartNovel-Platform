package com.myproject.novel.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节信息响应
 */
@Data
public class ChapterRespDto {

    private Long id;
    private Long bookId;
    private Integer chapterNum;
    private String chapterName;
    private Integer wordCount;
    private Integer isVip;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 章节正文（仅章节详情接口返回，列表接口为 null 不输出） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String content;
}