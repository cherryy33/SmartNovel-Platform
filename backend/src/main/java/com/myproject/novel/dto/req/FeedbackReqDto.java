package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户反馈请求
 */
@Data
public class FeedbackReqDto {

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 1000, message = "反馈内容最长 1000 个字")
    private String content;
}