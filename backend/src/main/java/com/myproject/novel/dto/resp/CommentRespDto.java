package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论响应
 */
@Data
public class CommentRespDto {

    private Long id;
    private Long bookId;
    private Long chapterId;
    private String chapterName;
    private Long userId;
    private String username;
    private String nickName;
    private String userPhoto;
    private String content;
    private Integer replyCount;
    private Integer auditStatus;
    private LocalDateTime createTime;
}