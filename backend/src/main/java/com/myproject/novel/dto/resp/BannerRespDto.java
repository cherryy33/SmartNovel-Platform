package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * 轮播图响应
 */
@Data
public class BannerRespDto {

    private Long id;
    private String title;
    private String imageUrl;
    private Long bookId;
    private Integer sort;
    private Integer status;
}