package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.dao.entity.AuthorInfo;
import com.myproject.novel.dao.entity.BookCategory;
import com.myproject.novel.dao.entity.BookInfo;
import com.myproject.novel.dao.entity.UserInfo;
import com.myproject.novel.dao.mapper.AuthorInfoMapper;
import com.myproject.novel.dao.mapper.BookCategoryMapper;
import com.myproject.novel.dao.mapper.BookInfoMapper;
import com.myproject.novel.dao.mapper.UserInfoMapper;
import com.myproject.novel.dto.resp.DashboardRespDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员仪表盘统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserInfoMapper userInfoMapper;
    private final AuthorInfoMapper authorInfoMapper;
    private final BookInfoMapper bookInfoMapper;
    private final BookCategoryMapper bookCategoryMapper;

    /**
     * 获取管理员仪表盘数据
     */
    public DashboardRespDto getDashboard() {
        DashboardRespDto dto = new DashboardRespDto();

        // 核心指标
        dto.setTotalUsers(userInfoMapper.selectCount(null));
        dto.setTotalAuthors(authorInfoMapper.selectCount(
            new LambdaQueryWrapper<AuthorInfo>().eq(AuthorInfo::getStatus, 1)));
        dto.setTotalBooks(bookInfoMapper.selectCount(null));
        dto.setTotalVipUsers(userInfoMapper.selectCount(
            new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getIsVip, 1)));

        // totalReads: sum visit_count from all books
        Long totalReads = 0L;
        List<BookInfo> allBooks = bookInfoMapper.selectList(
            new LambdaQueryWrapper<BookInfo>().select(BookInfo::getVisitCount));
        if (allBooks != null) {
            totalReads = allBooks.stream()
                .filter(b -> b != null)
                .map(b -> b.getVisitCount() == null ? 0L : b.getVisitCount())
                .reduce(0L, Long::sum);
        }
        dto.setTotalReads(totalReads);

        // 分类数量分布
        dto.setCategoryDistribution(buildCategoryDistribution());

        // 近7日趋势
        dto.setDailyReadTrend(buildDailyBookTrend());
        dto.setDailyNewUserTrend(buildDailyNewUserTrend());

        return dto;
    }

    /**
     * 分类数量分布：LEFT JOIN book_category + book_info，按 category_id 分组计数
     */
    private List<Map<String, Object>> buildCategoryDistribution() {
        List<BookCategory> categories = bookCategoryMapper.selectList(null);
        Map<Long, String> categoryNameMap = categories.stream()
            .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));

        // 按 categoryId 分组计数
        List<BookInfo> books = bookInfoMapper.selectList(
            new LambdaQueryWrapper<BookInfo>().select(BookInfo::getCategoryId));
        Map<Long, Long> countMap = new HashMap<>();
        if (books != null) {
            for (BookInfo book : books) {
                // 单列查询时，category_id 为 NULL 的行会被 MyBatis 映射为 null 元素
                if (book == null) {
                    continue;
                }
                Long catId = book.getCategoryId();
                if (catId != null) {
                    countMap.merge(catId, 1L, Long::sum);
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (BookCategory category : categories) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", category.getName());
            entry.put("value", countMap.getOrDefault(category.getId(), 0L));
            result.add(entry);
        }
        return result;
    }

    /**
     * 近7日每日新增小说数量趋势（简化替代阅读量趋势）
     */
    private List<Map<String, Object>> buildDailyBookTrend() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            Long count = bookInfoMapper.selectCount(
                new LambdaQueryWrapper<BookInfo>()
                    .ge(BookInfo::getCreateTime, dayStart)
                    .lt(BookInfo::getCreateTime, dayEnd));

            Map<String, Object> entry = new HashMap<>();
            entry.put("date", date.format(fmt));
            entry.put("value", count);
            trend.add(entry);
        }
        return trend;
    }

    /**
     * 近7日每日新增用户数量趋势
     */
    private List<Map<String, Object>> buildDailyNewUserTrend() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            Long count = userInfoMapper.selectCount(
                new LambdaQueryWrapper<UserInfo>()
                    .ge(UserInfo::getCreateTime, dayStart)
                    .lt(UserInfo::getCreateTime, dayEnd));

            Map<String, Object> entry = new HashMap<>();
            entry.put("date", date.format(fmt));
            entry.put("value", count);
            trend.add(entry);
        }
        return trend;
    }
}
