package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区回帖
 */
@Data
@TableName("community_reply")
public class CommunityReply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long userId;
    private String replyContent;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
