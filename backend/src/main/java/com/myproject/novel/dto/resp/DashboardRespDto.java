package com.myproject.novel.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 管理员仪表盘响应
 */
@Data
public class DashboardRespDto {

    /** 核心指标 */
    private Long totalUsers;
    private Long totalAuthors;
    private Long totalBooks;
    private Long totalReads;
    private Long totalVipUsers;

    /** 分类数量分布（饼状图）key=分类名 value=数量 */
    private List<Map<String, Object>> categoryDistribution;

    /** 近7日阅读量趋势（柱状图）key=日期 value=数量 */
    private List<Map<String, Object>> dailyReadTrend;

    /** 近7日新增用户趋势（柱状图）key=日期 value=数量 */
    private List<Map<String, Object>> dailyNewUserTrend;
}