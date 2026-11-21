package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员 AI 数据分析助手请求（Tool Calling）
 */
@Data
public class AdminAiAskReqDto {

    /** 管理员问题 */
    @NotBlank(message = "问题不能为空")
    private String question;
}
