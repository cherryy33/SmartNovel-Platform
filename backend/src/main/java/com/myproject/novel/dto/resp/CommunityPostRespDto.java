package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区帖子响应
 */
@Data
public class CommunityPostRespDto {

    private Long id;
    private Long userId;
    private String username;
    private String nickName;
    private String userPhoto;
    private String title;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Integer status;
    private LocalDateTime createTime;
}