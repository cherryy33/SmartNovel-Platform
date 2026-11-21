package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说 AI 知识库索引状态
 */
@Data
@TableName("book_ai_index")
public class BookAiIndex {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;
    private Integer indexedChapterNum;
    private Long indexedChapterId;
    private Integer chunkCount;
    private String embeddingModel;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
