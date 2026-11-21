package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 扩写/缩写/续写/润色请求
 */
@Data
public class AiExpandReqDto {

    @NotBlank(message = "正文内容不能为空")
    private String content;

    /** 操作类型：expand/shrink/continue/polish（不传默认 expand） */
    private String actionType;

    /** 可选的补充要求（如“增加景物描写”“让对白更自然”） */
    private String prompt;
}