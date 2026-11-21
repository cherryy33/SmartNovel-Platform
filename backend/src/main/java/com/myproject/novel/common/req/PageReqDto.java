package com.myproject.novel.common.req;

import lombok.Data;

/**
 * 分页请求基类
 */
@Data
public class PageReqDto {

    /** 页码，默认第 1 页 */
    private int pageNum = 1;

    /** 每页大小，默认 10 条 */
    private int pageSize = 10;
}
