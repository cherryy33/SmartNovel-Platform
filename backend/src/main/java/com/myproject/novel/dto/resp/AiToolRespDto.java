package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * AI Tool Calling 通用响应
 */
@Data
public class AiToolRespDto {

    private String answer;

    /** AI 写作助手的处理结果文本（扩写/缩写/续写/润色） */
    private String processedContent;
}