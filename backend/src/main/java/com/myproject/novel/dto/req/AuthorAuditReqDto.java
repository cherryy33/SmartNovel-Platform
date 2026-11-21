package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作家入驻审批请求
 */
@Data
public class AuthorAuditReqDto {

    /** true 通过 / false 驳回 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 驳回原因 */
    private String remark;
}
