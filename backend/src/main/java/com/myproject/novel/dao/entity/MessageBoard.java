package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台留言板
 */
@Data
@TableName("message_board")
public class MessageBoard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String content;
    private String reply;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
