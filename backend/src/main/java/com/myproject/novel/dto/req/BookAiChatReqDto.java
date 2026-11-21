package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 读者 AI 阅读助手问答请求
 */
@Data
public class BookAiChatReqDto {

    /** 读者问题 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 读者当前阅读到的章节号（用于只检索已读章节并增量构建索引） */
    private Integer chapterNum;

    /** 期望召回的章节片段数，默认取配置 top-k */
    private Integer topK;
}
