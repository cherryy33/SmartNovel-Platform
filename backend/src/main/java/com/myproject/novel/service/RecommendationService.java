package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.dao.entity.*;
import com.myproject.novel.dao.mapper.*;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.dto.resp.RankingRespDto;
import com.myproject.novel.dto.resp.RecommendRespDto;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务：Item-Based 协同过滤 + 兴趣标签推荐 + 热点兜底
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final BookInfoMapper bookInfoMapper;
    private final UserReadHistoryMapper userReadHistoryMapper;
    private final UserBookLikeMapper userBookLikeMapper;
    private final UserBookshelfMapper userBookshelfMapper;
    private final UserBookScoreMapper userBookScoreMapper;
    private final UserTagMapper userTagMapper;
    private final BookTagMapper bookTagMapper;
    private final TagMapper tagMapper;

    /**
     * 为当前用户生成推荐。未登录时返回热点推荐。
     */
    public RecommendRespDto recommendForUser() {
        var loginUser = UserContext.get();
        if (loginUser == null) {
            // 未登录：仅热点推荐
            List<BookInfoRespDto> hot = getHotBooks(12);
            return new RecommendRespDto(hot, Collections.emptyList(), Collections.emptyList());
        }
        Long userId = loginUser.getUserId();

        List<BookInfoRespDto> personal = itemBasedCF(userId);
        List<BookInfoRespDto> tagRec = tagBasedRecommend(userId);
        List<BookInfoRespDto> guess = guessYouLike(userId);

        // 各维度为空时填入热点
        if (personal.isEmpty()) personal = getHotBooks(6);
        if (tagRec.isEmpty()) tagRec = getHotBooks(6);
        if (guess.isEmpty()) guess = getHotBooks(6);

        return new RecommendRespDto(personal, tagRec, guess);
    }

    /**
     * 排行榜聚合
     */
    public RankingRespDto getRankings() {
        return new RankingRespDto(
            getHotBooks(10), getNewBooks(10), getScoreBooks(10), getFavoritedBooks(10));
    }

    // ==================== Item-Based CF ====================

    private List<BookInfoRespDto> itemBasedCF(Long userId) {
        // 1. 收集用户有过行为的书籍及行为权重
        Map<Long, Double> userRatings = new HashMap<>();
        collectBehavior(userId, userRatings);
        if (userRatings.isEmpty()) return Collections.emptyList();

        // 2. 获取用户已交互过的所有书籍id
        Set<Long> interactedBookIds = getUserInteractedBookIds(userId);

        // 3. 对每个preferred book, 找到同样喜欢该书的其他用户, 聚合候选书籍
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Map.Entry<Long, Double> entry : userRatings.entrySet()) {
            Long preferredBookId = entry.getKey();
            double userScore = entry.getValue();

            // 找到也喜欢这本书的其他用户
            Set<Long> similarUsers = findUsersWhoLikedBook(preferredBookId, userId);
            for (Long similarUserId : similarUsers) {
                // 找到该用户喜欢的其它书
                Map<Long, Double> similarUserRatings = new HashMap<>();
                collectBehavior(similarUserId, similarUserRatings);
                for (Map.Entry<Long, Double> se : similarUserRatings.entrySet()) {
                    Long candidateBookId = se.getKey();
                    if (interactedBookIds.contains(candidateBookId)) continue;
                    double similarity = se.getValue() * userScore;
                    candidateScores.merge(candidateBookId, similarity, Double::sum);
                }
            }
        }

        // 4. 取 top 12
        List<Long> topBookIds = candidateScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(12)
            .map(Map.Entry::getKey)
            .toList();
        return toBookRespList(topBookIds);
    }

    private void collectBehavior(Long userId, Map<Long, Double> ratings) {
        // 浏览 1分
        userReadHistoryMapper.selectList(
            new LambdaQueryWrapper<UserReadHistory>().eq(UserReadHistory::getUserId, userId))
            .forEach(h -> ratings.merge(h.getBookId(), 1.0, Double::sum));
        // 点赞 3分
        userBookLikeMapper.selectList(
            new LambdaQueryWrapper<UserBookLike>().eq(UserBookLike::getUserId, userId))
            .forEach(l -> ratings.merge(l.getBookId(), 3.0, Double::sum));
        // 收藏 5分
        userBookshelfMapper.selectList(
            new LambdaQueryWrapper<UserBookshelf>().eq(UserBookshelf::getUserId, userId))
            .forEach(s -> ratings.merge(s.getBookId(), 5.0, Double::sum));
        // 评分: score * 2
        userBookScoreMapper.selectList(
            new LambdaQueryWrapper<UserBookScore>().eq(UserBookScore::getUserId, userId))
            .forEach(s -> ratings.merge(s.getBookId(), s.getScore() * 2.0, Double::sum));
    }

    private Set<Long> getUserInteractedBookIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        userReadHistoryMapper.selectList(
            new LambdaQueryWrapper<UserReadHistory>().eq(UserReadHistory::getUserId, userId))
            .forEach(h -> ids.add(h.getBookId()));
        userBookLikeMapper.selectList(
            new LambdaQueryWrapper<UserBookLike>().eq(UserBookLike::getUserId, userId))
            .forEach(l -> ids.add(l.getBookId()));
        userBookshelfMapper.selectList(
            new LambdaQueryWrapper<UserBookshelf>().eq(UserBookshelf::getUserId, userId))
            .forEach(s -> ids.add(s.getBookId()));
        userBookScoreMapper.selectList(
            new LambdaQueryWrapper<UserBookScore>().eq(UserBookScore::getUserId, userId))
            .forEach(s -> ids.add(s.getBookId()));
        return ids;
    }

    private Set<Long> findUsersWhoLikedBook(Long bookId, Long excludeUserId) {
        Set<Long> users = new HashSet<>();
        userReadHistoryMapper.selectList(
            new LambdaQueryWrapper<UserReadHistory>().eq(UserReadHistory::getBookId, bookId))
            .forEach(h -> { if (!h.getUserId().equals(excludeUserId)) users.add(h.getUserId()); });
        userBookLikeMapper.selectList(
            new LambdaQueryWrapper<UserBookLike>().eq(UserBookLike::getBookId, bookId))
            .forEach(l -> { if (!l.getUserId().equals(excludeUserId)) users.add(l.getUserId()); });
        userBookshelfMapper.selectList(
            new LambdaQueryWrapper<UserBookshelf>().eq(UserBookshelf::getBookId, bookId))
            .forEach(s -> { if (!s.getUserId().equals(excludeUserId)) users.add(s.getUserId()); });
        return users;
    }

    // ==================== 兴趣标签推荐 ====================

    private List<BookInfoRespDto> tagBasedRecommend(Long userId) {
        // 获取用户已选的标签
        List<Long> userTagIds = userTagMapper.selectList(
            new LambdaQueryWrapper<UserTag>().eq(UserTag::getUserId, userId))
            .stream().map(UserTag::getTagId).toList();
        if (userTagIds.isEmpty()) return Collections.emptyList();

        // 找到包含这些标签的小说
        Set<Long> bookIds = new HashSet<>();
        for (Long tagId : userTagIds) {
            bookTagMapper.selectList(
                new LambdaQueryWrapper<BookTag>().eq(BookTag::getTagId, tagId))
                .forEach(bt -> bookIds.add(bt.getBookId()));
        }

        // 排除已读
        Set<Long> readIds = new HashSet<>();
        userReadHistoryMapper.selectList(
            new LambdaQueryWrapper<UserReadHistory>().eq(UserReadHistory::getUserId, userId))
            .forEach(h -> readIds.add(h.getBookId()));
        bookIds.removeAll(readIds);

        return toBookRespList(new ArrayList<>(bookIds).stream().limit(12).toList());
    }

    // ==================== 猜你喜欢 ====================

    private List<BookInfoRespDto> guessYouLike(Long userId) {
        // 简化：随机从高分书中取
        return getScoreBooks(12);
    }

    // ==================== 排行榜 ====================

    private List<BookInfoRespDto> getHotBooks(int limit) {
        return bookInfoMapper.selectList(
            new LambdaQueryWrapper<BookInfo>()
                .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED)
                .orderByDesc(BookInfo::getVisitCount)
                .last("LIMIT " + limit))
            .stream().map(this::toResp).toList();
    }

    private List<BookInfoRespDto> getNewBooks(int limit) {
        return bookInfoMapper.selectList(
            new LambdaQueryWrapper<BookInfo>()
                .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED)
                .orderByDesc(BookInfo::getCreateTime)
                .last("LIMIT " + limit))
            .stream().map(this::toResp).toList();
    }

    private List<BookInfoRespDto> getScoreBooks(int limit) {
        return bookInfoMapper.selectList(
            new LambdaQueryWrapper<BookInfo>()
                .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED)
                .orderByDesc(BookInfo::getScore)
                .last("LIMIT " + limit))
            .stream().map(this::toResp).toList();
    }

    private List<BookInfoRespDto> getFavoritedBooks(int limit) {
        List<Map<String, Object>> topMaps = userBookshelfMapper.selectMaps(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBookshelf>()
                .select("book_id, count(1) as cnt")
                .groupBy("book_id").orderByDesc("cnt").last("LIMIT " + limit));
        if (topMaps.isEmpty()) return Collections.emptyList();
        // MySQL 无符号 bigint 经 selectMaps 返回的是 BigInteger，需按 Number 统一取值再转 long
        List<Long> ids = topMaps.stream()
            .map(m -> ((Number) m.get("book_id")).longValue())
            .toList();
        return toBookRespList(ids);
    }

    // ==================== 工具方法 ====================

    private List<BookInfoRespDto> toBookRespList(List<Long> bookIds) {
        if (bookIds.isEmpty()) return Collections.emptyList();
        List<BookInfo> books = bookInfoMapper.selectList(
            new LambdaQueryWrapper<BookInfo>()
                .in(BookInfo::getId, bookIds)
                .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED));
        Map<Long, BookInfo> bookMap = books.stream()
            .collect(Collectors.toMap(BookInfo::getId, b -> b));
        List<BookInfoRespDto> result = new ArrayList<>();
        for (Long id : bookIds) {
            BookInfo book = bookMap.get(id);
            if (book != null) result.add(toResp(book));
        }
        return result;
    }

    private BookInfoRespDto toResp(BookInfo book) {
        BookInfoRespDto dto = new BookInfoRespDto();
        dto.setId(book.getId()); dto.setBookName(book.getBookName());
        dto.setAuthorName(book.getAuthorName()); dto.setPicUrl(book.getPicUrl());
        dto.setCategoryName(book.getCategoryName()); dto.setBookDesc(book.getBookDesc());
        dto.setScore(book.getScore()); dto.setVisitCount(book.getVisitCount());
        dto.setWordCount(book.getWordCount()); dto.setIsVip(book.getIsVip());
        dto.setBookStatus(book.getBookStatus()); dto.setLastChapterName(book.getLastChapterName());
        dto.setCreateTime(book.getCreateTime());
        return dto;
    }
}
