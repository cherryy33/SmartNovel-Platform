package com.myproject.novel.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 排行榜聚合响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingRespDto {

    /** 热度榜 */
    private List<BookInfoRespDto> hotRanking;

    /** 新书榜 */
    private List<BookInfoRespDto> newBookRanking;

    /** 高分榜 */
    private List<BookInfoRespDto> scoreRanking;

    /** 收藏榜 */
    private List<BookInfoRespDto> favoritedRanking;
}