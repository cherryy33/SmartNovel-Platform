package com.myproject.novel.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 读者 AI 阅读助手问答 响应 DTO
 */
@Data
@Builder
public class BookAiChatRespDto {

    /** AI 回答文本 */
    private String answer;

    /** 当前索引状态 */
    private Integer indexStatus;

    /** 引用来源章节列表 */
    private List<BookAiSourceDto> sources;
}
