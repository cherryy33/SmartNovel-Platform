package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 作者 AI 助手问答请求（Tool Calling）
 */
@Data
public class AuthorAiAskReqDto {

    /** 作者问题 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 重点针对的作品 ID（可选） */
    private Long bookId;
}
