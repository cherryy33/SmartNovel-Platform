package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为记录响应（管理后台查看）
 */
@Data
public class UserActionRespDto {

    private Long id;
    private Long userId;
    private String username;
    private String nickName;
    private Long bookId;
    private String bookName;
    private String actionType;
    private Integer score;
    private LocalDateTime createTime;
}