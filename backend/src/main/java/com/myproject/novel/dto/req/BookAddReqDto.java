package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增小说请求
 */
@Data
public class BookAddReqDto {

    @NotBlank(message = "小说名不能为空")
    @Size(max = 50, message = "小说名最长 50 个字")
    private String bookName;

    @Size(max = 2000, message = "书籍描述最长 2000 个字")
    private String bookDesc;

    private Long categoryId;
    private Integer workDirection;

    /** 是否 VIP 作品 */
    private Integer isVip;

    /** 标签 ID 列表 */
    private List<Long> tagIds;

    /** 小说状态 0-连载中 1-已完结 */
    private Integer bookStatus;
}
