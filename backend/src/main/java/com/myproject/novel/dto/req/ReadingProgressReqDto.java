package com.myproject.novel.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 阅读进度保存请求
 */
@Data
public class ReadingProgressReqDto {

    /** 章节 ID（兼容前端旧字段名 chapterId） */
    @JsonAlias("chapterId")
    private Long lastChapterId;
}
