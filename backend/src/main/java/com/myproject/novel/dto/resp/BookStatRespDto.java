package com.myproject.novel.dto.resp;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 单本小说数据分析响应
 */
@Data
public class BookStatRespDto {

    /** 评分分布：key=1-5星 value=人数 */
    private List<Map<String, Object>> scoreDistribution;

    /** 阅读趋势：key=日期 value=阅读量 */
    private List<Map<String, Object>> readTrend;
}