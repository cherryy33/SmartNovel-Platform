package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.dao.entity.BookInfo;
import com.myproject.novel.dao.entity.BookTag;
import com.myproject.novel.dao.entity.Tag;
import com.myproject.novel.dao.entity.UserBookLike;
import com.myproject.novel.dao.entity.UserBookScore;
import com.myproject.novel.dao.entity.UserBookshelf;
import com.myproject.novel.dao.mapper.BookInfoMapper;
import com.myproject.novel.dao.mapper.BookTagMapper;
import com.myproject.novel.dao.mapper.TagMapper;
import com.myproject.novel.dao.mapper.UserBookLikeMapper;
import com.myproject.novel.dao.mapper.UserBookScoreMapper;
import com.myproject.novel.dao.mapper.UserBookshelfMapper;
import com.myproject.novel.dto.req.SearchReqDto;
import com.myproject.novel.dto.req.BookAddReqDto;
import com.myproject.novel.dto.req.BookUpdateReqDto;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.security.LoginUser;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 小说服务
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookInfoMapper bookInfoMapper;
    private final BookTagMapper bookTagMapper;
    private final TagMapper tagMapper;
    private final UserBookshelfMapper userBookshelfMapper;
    private final UserBookLikeMapper userBookLikeMapper;
    private final UserBookScoreMapper userBookScoreMapper;

    /**
     * 分页查询已发布的小说列表（审核通过）
     */
    public PageRespDto<BookInfoRespDto> listPublishedBooks(SearchReqDto dto) {
        LambdaQueryWrapper<BookInfo> wrapper = new LambdaQueryWrapper<BookInfo>()
                .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED);

        // 关键词过滤（小说名）
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.like(BookInfo::getBookName, dto.getKeyword());
        }
        // 分类过滤
        if (dto.getCategoryId() != null) {
            wrapper.eq(BookInfo::getCategoryId, dto.getCategoryId());
        }
        // 作品方向过滤
        if (dto.getWorkDirection() != null) {
            wrapper.eq(BookInfo::getWorkDirection, dto.getWorkDirection());
        }
        // VIP 过滤
        if (dto.getIsVip() != null) {
            wrapper.eq(BookInfo::getIsVip, dto.getIsVip());
        }

        // 排序
        applySorting(wrapper, dto);

        IPage<BookInfo> page = bookInfoMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);

        List<BookInfoRespDto> list = page.getRecords().stream()
                .map(this::toRespDto)
                .collect(Collectors.toList());

        return PageRespDto.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }

    /**
     * 获取小说详情（含标签 + 当前用户交互状态）
     */
    public BookInfoRespDto getBookDetail(Long bookId) {
        BookInfo book = bookInfoMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        BookInfoRespDto dto = toRespDto(book);
        dto.setTags(getBookTagNames(bookId));

        // 填充当前用户的交互状态（已登录时）
        LoginUser loginUser = UserContext.get();
        if (loginUser != null) {
            Long userId = loginUser.getUserId();
            // 是否收藏
            dto.setIsFavorited(userBookshelfMapper.selectCount(
                    new LambdaQueryWrapper<UserBookshelf>()
                            .eq(UserBookshelf::getUserId, userId)
                            .eq(UserBookshelf::getBookId, bookId)) > 0);
            // 是否点赞
            dto.setIsLiked(userBookLikeMapper.selectCount(
                    new LambdaQueryWrapper<UserBookLike>()
                            .eq(UserBookLike::getUserId, userId)
                            .eq(UserBookLike::getBookId, bookId)) > 0);
            // 我的评分
            UserBookScore myScore = userBookScoreMapper.selectOne(
                    new LambdaQueryWrapper<UserBookScore>()
                            .eq(UserBookScore::getUserId, userId)
                            .eq(UserBookScore::getBookId, bookId));
            dto.setMyScore(myScore != null ? myScore.getScore() : null);
        }

        return dto;
    }

    /**
     * 搜索小说（按书名或作者名模糊匹配）
     */
    public PageRespDto<BookInfoRespDto> searchBooks(SearchReqDto dto) {
        LambdaQueryWrapper<BookInfo> wrapper = new LambdaQueryWrapper<BookInfo>()
                .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED);

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(BookInfo::getBookName, dto.getKeyword())
                    .or()
                    .like(BookInfo::getAuthorName, dto.getKeyword()));
        }
        if (dto.getCategoryId() != null) {
            wrapper.eq(BookInfo::getCategoryId, dto.getCategoryId());
        }
        if (dto.getWorkDirection() != null) {
            wrapper.eq(BookInfo::getWorkDirection, dto.getWorkDirection());
        }
        if (dto.getIsVip() != null) {
            wrapper.eq(BookInfo::getIsVip, dto.getIsVip());
        }

        applySorting(wrapper, dto);

        IPage<BookInfo> page = bookInfoMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);

        List<BookInfoRespDto> list = page.getRecords().stream()
                .map(this::toRespDto)
                .collect(Collectors.toList());

        return PageRespDto.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }

    /**
     * 热门小说（按访问量排序）
     */
    public List<BookInfoRespDto> getHotBooks(int limit) {
        List<BookInfo> books = bookInfoMapper.selectList(
                new LambdaQueryWrapper<BookInfo>()
                        .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED)
                        .orderByDesc(BookInfo::getVisitCount)
                        .last("LIMIT " + limit));
        return books.stream().map(this::toRespDto).collect(Collectors.toList());
    }

    /**
     * 最新小说（按创建时间排序）
     */
    public List<BookInfoRespDto> getNewBooks(int limit) {
        List<BookInfo> books = bookInfoMapper.selectList(
                new LambdaQueryWrapper<BookInfo>()
                        .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED)
                        .orderByDesc(BookInfo::getCreateTime)
                        .last("LIMIT " + limit));
        return books.stream().map(this::toRespDto).collect(Collectors.toList());
    }

    /**
     * 高分小说（按评分排序）
     */
    public List<BookInfoRespDto> getScoreBooks(int limit) {
        List<BookInfo> books = bookInfoMapper.selectList(
                new LambdaQueryWrapper<BookInfo>()
                        .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED)
                        .orderByDesc(BookInfo::getScore)
                        .last("LIMIT " + limit));
        return books.stream().map(this::toRespDto).collect(Collectors.toList());
    }

    /**
     * 收藏最多的小说
     */
    public List<BookInfoRespDto> getFavoritedBooks(int limit) {
        // 按 bookId 分组统计收藏数，取最多的 N 个
        List<Map<String, Object>> topMaps = userBookshelfMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserBookshelf>()
                        .select("book_id, count(1) as cnt")
                        .groupBy("book_id")
                        .orderByDesc("cnt")
                        .last("LIMIT " + limit));

        if (topMaps.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> bookIds = topMaps.stream()
                .map(m -> toLong(m.get("book_id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<BookInfo> books = bookInfoMapper.selectList(
                new LambdaQueryWrapper<BookInfo>()
                        .in(BookInfo::getId, bookIds)
                        .eq(BookInfo::getAuditStatus, CommonConsts.AuditStatus.PUBLISHED));

        // 按收藏数降序排列
        Map<Long, BookInfo> bookMap = books.stream()
                .collect(Collectors.toMap(BookInfo::getId, b -> b));

        List<BookInfoRespDto> result = new ArrayList<>();
        for (Long bookId : bookIds) {
            BookInfo book = bookMap.get(bookId);
            if (book != null) {
                result.add(toRespDto(book));
            }
        }
        return result;
    }

    /**
     * 兼容 JDBC 驱动对聚合查询返回的不同数字类型（BigInteger / BigDecimal / Long / Integer 等）
     */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(val).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 作者侧 CRUD ====================

    /** 列出当前作者的所有小说 */
    public List<BookInfoRespDto> listMyBooks() {
        Long authorId = UserContext.require().getAuthorId();
        List<BookInfo> books = bookInfoMapper.selectList(
            new LambdaQueryWrapper<BookInfo>()
                .eq(BookInfo::getAuthorId, authorId)
                .orderByDesc(BookInfo::getUpdateTime));
        return books.stream().map(this::toRespDto).collect(Collectors.toList());
    }

    /** 新增小说 */
    @Transactional
    public void addBook(BookAddReqDto dto) {
        Long authorId = UserContext.require().getAuthorId();
        String authorName = UserContext.require().getUsername();
        BookInfo book = new BookInfo();
        book.setWorkDirection(dto.getWorkDirection() != null ? dto.getWorkDirection() : 0);
        book.setCategoryId(dto.getCategoryId());
        book.setBookName(dto.getBookName());
        book.setAuthorId(authorId);
        book.setAuthorName(authorName);
        book.setBookDesc(dto.getBookDesc());
        book.setIsVip(dto.getIsVip() != null ? dto.getIsVip() : CommonConsts.NO);
        book.setBookStatus(CommonConsts.NO);
        book.setAuditStatus(CommonConsts.AuditStatus.PENDING);
        book.setScore(0);
        book.setVisitCount(0L);
        book.setWordCount(0);
        book.setCommentCount(0);
        book.setCreateTime(LocalDateTime.now());
        book.setUpdateTime(LocalDateTime.now());
        bookInfoMapper.insert(book);
        // 关联标签
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                BookTag bt = new BookTag();
                bt.setBookId(book.getId());
                bt.setTagId(tagId);
                bookTagMapper.insert(bt);
            }
        }
    }

    /** 更新小说 */
    @Transactional
    public void updateBook(Long bookId, BookUpdateReqDto dto) {
        BookInfo book = bookInfoMapper.selectById(bookId);
        if (book == null) throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        Long authorId = UserContext.require().getAuthorId();
        if (!authorId.equals(book.getAuthorId()))
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION);
        if (dto.getBookName() != null) book.setBookName(dto.getBookName());
        if (dto.getCategoryId() != null) book.setCategoryId(dto.getCategoryId());
        if (dto.getBookDesc() != null) book.setBookDesc(dto.getBookDesc());
                if (dto.getIsVip() != null) book.setIsVip(dto.getIsVip());
        if (dto.getBookStatus() != null) book.setBookStatus(dto.getBookStatus());
        if (dto.getWorkDirection() != null) book.setWorkDirection(dto.getWorkDirection());
        book.setUpdateTime(LocalDateTime.now());
        bookInfoMapper.updateById(book);
        // 更新标签
        if (dto.getTagIds() != null) {
            bookTagMapper.delete(new LambdaQueryWrapper<BookTag>().eq(BookTag::getBookId, bookId));
            for (Long tagId : dto.getTagIds()) {
                BookTag bt = new BookTag();
                bt.setBookId(bookId);
                bt.setTagId(tagId);
                bookTagMapper.insert(bt);
            }
        }
    }

    /** 删除小说 */
    @Transactional
    public void deleteBook(Long bookId) {
        BookInfo book = bookInfoMapper.selectById(bookId);
        if (book == null) throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        Long authorId = UserContext.require().getAuthorId();
        if (!authorId.equals(book.getAuthorId()))
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION);
        bookTagMapper.delete(new LambdaQueryWrapper<BookTag>().eq(BookTag::getBookId, bookId));
        bookInfoMapper.deleteById(bookId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 BookInfo 实体转换为响应 DTO（不含标签和用户交互状态）
     */
    private BookInfoRespDto toRespDto(BookInfo book) {
        BookInfoRespDto dto = new BookInfoRespDto();
        dto.setId(book.getId());
        dto.setWorkDirection(book.getWorkDirection());
        dto.setCategoryId(book.getCategoryId());
        dto.setCategoryName(book.getCategoryName());
        dto.setPicUrl(book.getPicUrl());
        dto.setBookName(book.getBookName());
        dto.setAuthorId(book.getAuthorId());
        dto.setAuthorName(book.getAuthorName());
        dto.setBookDesc(book.getBookDesc());
        dto.setScore(book.getScore());
        dto.setBookStatus(book.getBookStatus());
        dto.setVisitCount(book.getVisitCount());
        dto.setWordCount(book.getWordCount());
        dto.setCommentCount(book.getCommentCount());
        dto.setLastChapterId(book.getLastChapterId());
        dto.setLastChapterName(book.getLastChapterName());
        dto.setLastChapterUpdateTime(book.getLastChapterUpdateTime());
        dto.setIsVip(book.getIsVip());
        dto.setAuditStatus(book.getAuditStatus());
        dto.setCreateTime(book.getCreateTime());
        return dto;
    }

    /**
     * 获取某小说的标签名称列表
     */
    private List<String> getBookTagNames(Long bookId) {
        List<BookTag> bookTags = bookTagMapper.selectList(
                new LambdaQueryWrapper<BookTag>()
                        .eq(BookTag::getBookId, bookId));
        if (bookTags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = bookTags.stream()
                .map(BookTag::getTagId)
                .collect(Collectors.toList());
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .in(Tag::getId, tagIds));
        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }

    /**
     * 根据 SearchReqDto 中的 sortField / sortOrder 应用排序
     */
    private void applySorting(LambdaQueryWrapper<BookInfo> wrapper, SearchReqDto dto) {
        String sortField = dto.getSortField();
        boolean asc = "asc".equalsIgnoreCase(dto.getSortOrder());

        if ("visit_count".equals(sortField)) {
            wrapper.orderBy(true, asc, BookInfo::getVisitCount);
        } else if ("score".equals(sortField)) {
            wrapper.orderBy(true, asc, BookInfo::getScore);
        } else if ("create_time".equals(sortField)) {
            wrapper.orderBy(true, asc, BookInfo::getCreateTime);
        } else {
            // 默认按更新时间降序
            wrapper.orderByDesc(BookInfo::getUpdateTime);
        }
    }
}
