package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增章节请求
 */
@Data
public class ChapterAddReqDto {

    @NotBlank(message = "章节名不能为空")
    @Size(max = 100, message = "章节名最长 100 个字")
    private String chapterName;

    @NotBlank(message = "章节内容不能为空")
    private String content;

    /** 是否 VIP 章节 */
    private Integer isVip;
}
