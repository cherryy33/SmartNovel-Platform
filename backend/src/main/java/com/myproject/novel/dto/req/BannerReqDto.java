package com.myproject.novel.dto.req;

import lombok.Data;

/**
 * 轮播图请求
 */
@Data
public class BannerReqDto {

    private String title;
    private String imageUrl;
    private Long bookId;
    private Integer sort;
    private Integer status;
}