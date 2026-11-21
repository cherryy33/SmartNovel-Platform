package com.myproject.novel.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说 AI 知识库索引状态 响应 DTO
 */
@Data
@Builder
public class BookAiIndexRespDto {

    private Long bookId;

    /** 索引状态：0-未构建 1-构建中 2-已就绪 3-构建失败 */
    private Integer status;

    private String statusDesc;

    /** 已索引到的最大章节号 */
    private Integer indexedChapterNum;

    /** 全书总章节数 */
    private Long totalChapterNum;

    /** 向量块总数 */
    private Integer chunkCount;

    private String embeddingModel;

    private String errorMsg;

    private LocalDateTime updateTime;
}
