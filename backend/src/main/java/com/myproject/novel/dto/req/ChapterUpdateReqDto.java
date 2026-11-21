package com.myproject.novel.dto.req;

import lombok.Data;

/**
 * 编辑章节请求
 */
@Data
public class ChapterUpdateReqDto {

    private String chapterName;
    private String content;
    private Integer isVip;
}
