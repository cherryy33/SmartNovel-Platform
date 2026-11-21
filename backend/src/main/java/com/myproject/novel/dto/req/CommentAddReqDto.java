package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加评论请求
 */
@Data
public class CommentAddReqDto {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容最长 1000 个字")
    private String content;

    /** 章节 ID，为空则为书评 */
    private Long chapterId;
}
