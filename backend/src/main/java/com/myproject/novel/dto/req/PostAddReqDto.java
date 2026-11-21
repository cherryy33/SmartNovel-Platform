package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布社区帖子请求
 */
@Data
public class PostAddReqDto {

    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 200, message = "标题最长 200 个字")
    private String title;

    @NotBlank(message = "帖子内容不能为空")
    private String content;
}
