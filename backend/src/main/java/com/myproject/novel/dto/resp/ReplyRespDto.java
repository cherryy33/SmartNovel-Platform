package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论回复响应
 */
@Data
public class ReplyRespDto {

    private Long id;
    private Long commentId;
    private Long userId;
    private String username;
    private String nickName;
    private String userPhoto;
    private String replyContent;
    private Integer auditStatus;
    private LocalDateTime createTime;
}