package com.myproject.novel.dto.req;

import com.myproject.novel.common.req.PageReqDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 搜索请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SearchReqDto extends PageReqDto {

    /** 搜索关键词（小说名/作者名） */
    private String keyword;

    /** 分类 ID */
    private Long categoryId;

    /** 作品方向 0-男频 1-女频 */
    private Integer workDirection;

    /** VIP 筛选 0-免费 1-VIP */
    private Integer isVip;

    /** 排序字段：visit_count/create_time/score */
    private String sortField;

    /** 排序方向：asc/desc */
    private String sortOrder;
}