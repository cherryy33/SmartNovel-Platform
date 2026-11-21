package com.myproject.novel.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐结果响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRespDto {

    /** 个性化推荐（协同过滤） */
    private List<BookInfoRespDto> personalRecommend;

    /** 兴趣标签推荐 */
    private List<BookInfoRespDto> tagRecommend;

    /** 猜你喜欢（向量相似度） */
    private List<BookInfoRespDto> guessYouLike;
}