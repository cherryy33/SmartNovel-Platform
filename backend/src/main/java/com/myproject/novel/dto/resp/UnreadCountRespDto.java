package com.myproject.novel.dto.resp;

import lombok.Data;

/**
 * 未读数统计响应
 */
@Data
public class UnreadCountRespDto {

    /** 未读私信数 */
    private Long unreadMessages;

    /** 未读评论回复数（TODO: 后续扩展） */
    private Long unreadReplies;
}