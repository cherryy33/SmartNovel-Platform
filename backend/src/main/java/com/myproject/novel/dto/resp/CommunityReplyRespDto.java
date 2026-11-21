package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区回帖响应
 */
@Data
public class CommunityReplyRespDto {

    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String nickName;
    private String userPhoto;
    private String replyContent;
    private Integer status;
    private LocalDateTime createTime;
}