package com.myproject.novel.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信响应
 */
@Data
public class PrivateMessageRespDto {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderPhoto;
    private Long receiverId;
    private String receiverName;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}