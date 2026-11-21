package com.myproject.novel.dto.req;

import lombok.Data;

/**
 * 系统公告请求
 */
@Data
public class NoticeReqDto {

    private String title;
    private String content;
    private Integer status;
}