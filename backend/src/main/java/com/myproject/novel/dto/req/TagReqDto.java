package com.myproject.novel.dto.req;

import lombok.Data;

/**
 * 标签请求
 */
@Data
public class TagReqDto {

    private String name;
    private Integer sort;
}