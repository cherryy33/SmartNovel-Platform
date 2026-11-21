package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者信息响应
 */
@Data
public class AuthorInfoRespDto {

    private Long id;
    private Long userId;
    /** 申请人用户名（管理后台展示用） */
    private String username;
    private String penName;
    private String intro;
    private Integer status;
    /** 状态中文描述：未提交 / 待审核 / 已通过 / 已驳回 */
    private String statusText;
    private String reviewComment;
    private LocalDateTime createTime;
}