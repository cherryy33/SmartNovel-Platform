package com.myproject.novel.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小说 AI 知识库分块
 */
@Data
@TableName("book_ai_chunk")
public class BookAiChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookId;
    private Long chapterId;
    private Integer chapterNum;
    private String chapterName;
    private Integer chunkIndex;
    private String content;
    private Integer charCount;
    private String vectorId;
    private LocalDateTime createTime;
}
