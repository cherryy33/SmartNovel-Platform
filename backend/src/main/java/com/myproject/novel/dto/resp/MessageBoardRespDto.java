package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言板响应
 */
@Data
public class MessageBoardRespDto {

    private Long id;
    private Long userId;
    private String username;
    private String nickName;
    private String userPhoto;
    private String content;
    private String reply;
    private Integer status;
    private LocalDateTime createTime;
}