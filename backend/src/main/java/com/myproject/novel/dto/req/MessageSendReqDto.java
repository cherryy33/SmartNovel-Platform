package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送私信请求
 */
@Data
public class MessageSendReqDto {

    @NotBlank(message = "私信内容不能为空")
    @Size(max = 1000, message = "私信内容最长 1000 个字")
    private String content;
}
