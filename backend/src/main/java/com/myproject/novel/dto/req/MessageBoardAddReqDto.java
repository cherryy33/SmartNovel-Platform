package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 留言板留言请求
 */
@Data
public class MessageBoardAddReqDto {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 1000, message = "留言内容最长 1000 个字")
    private String content;
}