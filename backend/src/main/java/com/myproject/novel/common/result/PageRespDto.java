package com.myproject.novel.common.result;

import lombok.Getter;

import java.util.List;

/**
 * 分页响应体
 */
@Getter
public class PageRespDto<T> {

    private final long pageNum;
    private final long pageSize;
    private final long total;
    private final long pages;
    private final List<T> list;

    public PageRespDto(long pageNum, long pageSize, long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
        this.list = list;
    }

    public static <T> PageRespDto<T> of(long pageNum, long pageSize, long total, List<T> list) {
        return new PageRespDto<>(pageNum, pageSize, total, list);
    }
}
