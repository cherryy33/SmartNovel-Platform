package com.myproject.novel.dto.req;

import lombok.Data;

/**
 * 分类请求
 */
@Data
public class CategoryReqDto {

    private String name;
    private Integer workDirection;
    private Integer sort;
}