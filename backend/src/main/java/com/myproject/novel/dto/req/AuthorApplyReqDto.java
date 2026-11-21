package com.myproject.novel.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 作者入驻申请请求
 */
@Data
public class AuthorApplyReqDto {

    @NotBlank(message = "笔名不能为空")
    @Size(max = 50, message = "笔名最长 50 个字")
    private String penName;

    @Size(max = 500, message = "作者简介最长 500 个字")
    private String intro;
}
