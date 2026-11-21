package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告响应
 */
@Data
public class NoticeRespDto {

    private Long id;
    private String title;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}