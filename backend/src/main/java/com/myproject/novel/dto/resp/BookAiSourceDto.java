package com.myproject.novel.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 回答的引用来源（跳转回阅读页的锚点）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAiSourceDto {

    private Long chapterId;

    private Integer chapterNum;

    private String chapterName;

    /** 章节内容片段摘录 */
    private String snippet;

    /** 向量相似度得分 */
    private Double score;
}
