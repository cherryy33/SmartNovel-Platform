package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * 分类响应
 */
@Data
public class CategoryRespDto {

    private Long id;
    private String name;
    private Integer workDirection;
    private Integer sort;
}