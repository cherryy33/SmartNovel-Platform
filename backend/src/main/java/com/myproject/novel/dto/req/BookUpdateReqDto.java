package com.myproject.novel.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 编辑小说请求
 */
@Data
public class BookUpdateReqDto {

    private String bookName;
    private String bookDesc;
    private Long categoryId;
    private Integer workDirection;
    private Integer isVip;
    private List<Long> tagIds;
    private Integer bookStatus;
}
