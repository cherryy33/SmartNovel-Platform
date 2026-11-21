package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * 图形验证码响应
 */
@Data
public class CaptionRespDto {

    private String captchaKey;
    private String captchaImage;
}