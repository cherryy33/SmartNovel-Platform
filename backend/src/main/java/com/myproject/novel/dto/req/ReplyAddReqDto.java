package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加回复请求
 */
@Data
public class ReplyAddReqDto {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容最长 1000 个字")
    private String replyContent;
}
