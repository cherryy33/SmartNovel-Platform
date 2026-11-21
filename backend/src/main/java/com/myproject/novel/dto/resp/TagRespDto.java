package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * 标签响应
 */
@Data
public class TagRespDto {

    private Long id;
    private String name;
    private Integer sort;
}