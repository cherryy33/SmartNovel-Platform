package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核请求
 */
@Data
public class AuditReqDto {

    @NotNull(message = "审核状态不能为空")
    private Integer status;

    /** 审核意见 */
    private String reviewComment;
}