package com.myproject.novel.dto.req;

import lombok.Data;

/**
 * 修改用户资料请求
 */
@Data
public class UpdateUserReqDto {

    private String nickName;
    private String userPhoto;
    private Integer userSex;
    private String email;
    private String phone;
}
