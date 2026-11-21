package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论回复
 */
@Data
@TableName("comment_reply")
public class CommentReply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;
    private Long userId;
    private String replyContent;
    private Integer auditStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
