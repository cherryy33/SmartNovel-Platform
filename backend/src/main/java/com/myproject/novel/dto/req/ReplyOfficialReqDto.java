package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 官方回复请求（留言板/反馈）
 */
@Data
public class ReplyOfficialReqDto {

    @NotBlank(message = "回复内容不能为空")
    private String reply;
}