package com.myproject.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.exception.BusinessException;
import com.myproject.novel.common.exception.ErrorCode;
import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.dao.entity.BookChapter;
import com.myproject.novel.dao.entity.BookInfo;
import com.myproject.novel.dao.entity.PrivateMessage;
import com.myproject.novel.dao.entity.UserBookLike;
import com.myproject.novel.dao.entity.UserBookScore;
import com.myproject.novel.dao.entity.UserBookshelf;
import com.myproject.novel.dao.entity.UserInfo;
import com.myproject.novel.dao.entity.UserReadHistory;
import com.myproject.novel.dao.mapper.BookChapterMapper;
import com.myproject.novel.dao.mapper.BookInfoMapper;
import com.myproject.novel.dao.mapper.PrivateMessageMapper;
import com.myproject.novel.dao.mapper.UserBookLikeMapper;
import com.myproject.novel.dao.mapper.UserBookScoreMapper;
import com.myproject.novel.dao.mapper.UserBookshelfMapper;
import com.myproject.novel.dao.mapper.UserInfoMapper;
import com.myproject.novel.dao.mapper.UserReadHistoryMapper;
import com.myproject.novel.dto.req.MessageSendReqDto;
import com.myproject.novel.dto.req.ScoreReqDto;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.dto.resp.PrivateMessageRespDto;
import com.myproject.novel.dto.resp.ReadHistoryRespDto;
import com.myproject.novel.dto.resp.UnreadCountRespDto;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户互动服务：点赞、书架、评分、阅读历史、私信
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final UserBookLikeMapper userBookLikeMapper;
    private final UserBookshelfMapper userBookshelfMapper;
    private final UserBookScoreMapper userBookScoreMapper;
    private final UserReadHistoryMapper userReadHistoryMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final BookInfoMapper bookInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final BookChapterMapper bookChapterMapper;

    // ==================== 点赞 ====================
    @Transactional
    public void likeBook(Long bookId) {
        Long userId = UserContext.userId();
        UserBookLike existing = userBookLikeMapper.selectOne(
            new LambdaQueryWrapper<UserBookLike>()
                .eq(UserBookLike::getUserId, userId)
                .eq(UserBookLike::getBookId, bookId));
        if (existing != null) {
            userBookLikeMapper.deleteById(existing.getId());
        } else {
            UserBookLike like = new UserBookLike();
            like.setUserId(userId); like.setBookId(bookId);
            like.setCreateTime(LocalDateTime.now());
            userBookLikeMapper.insert(like);
        }
    }

    public boolean isLiked(Long bookId) {
        var loginUser = UserContext.get();
        if (loginUser == null) return false;
        return userBookLikeMapper.selectCount(
            new LambdaQueryWrapper<UserBookLike>()
                .eq(UserBookLike::getUserId, loginUser.getUserId())
                .eq(UserBookLike::getBookId, bookId)) > 0;
    }

    // ==================== 书架（收藏）====================
    @Transactional
    public void addToBookshelf(Long bookId) {
        Long userId = UserContext.userId();
        UserBookshelf existing = userBookshelfMapper.selectOne(
            new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getBookId, bookId));
        if (existing != null) {
            userBookshelfMapper.deleteById(existing.getId());
        } else {
            UserBookshelf shelf = new UserBookshelf();
            shelf.setUserId(userId); shelf.setBookId(bookId);
            shelf.setCreateTime(LocalDateTime.now());
            shelf.setUpdateTime(LocalDateTime.now());
            userBookshelfMapper.insert(shelf);
        }
    }

    public void removeFromBookshelf(Long bookId) {
        Long userId = UserContext.userId();
        userBookshelfMapper.delete(
            new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, userId)
                .eq(UserBookshelf::getBookId, bookId));
    }

    public boolean isFavorited(Long bookId) {
        var loginUser = UserContext.get();
        if (loginUser == null) return false;
        return userBookshelfMapper.selectCount(
            new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, loginUser.getUserId())
                .eq(UserBookshelf::getBookId, bookId)) > 0;
    }

    public PageRespDto<BookInfoRespDto> getBookshelf(PageReqDto page) {
        Long userId = UserContext.userId();
        IPage<UserBookshelf> result = userBookshelfMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<UserBookshelf>()
                .eq(UserBookshelf::getUserId, userId)
                .orderByDesc(UserBookshelf::getCreateTime));
        List<BookInfoRespDto> list = result.getRecords().stream()
            .map(s -> toBookResp(bookInfoMapper.selectById(s.getBookId())))
            .filter(Objects::nonNull).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    // ==================== 评分 ====================
    @Transactional
    public void scoreBook(Long bookId, ScoreReqDto dto) {
        Long userId = UserContext.userId();
        UserBookScore existing = userBookScoreMapper.selectOne(
            new LambdaQueryWrapper<UserBookScore>()
                .eq(UserBookScore::getUserId, userId)
                .eq(UserBookScore::getBookId, bookId));
        if (existing != null) {
            existing.setScore(dto.getScore());
            existing.setUpdateTime(LocalDateTime.now());
            userBookScoreMapper.updateById(existing);
        } else {
            UserBookScore score = new UserBookScore();
            score.setUserId(userId); score.setBookId(bookId);
            score.setScore(dto.getScore());
            score.setCreateTime(LocalDateTime.now());
            score.setUpdateTime(LocalDateTime.now());
            userBookScoreMapper.insert(score);
        }
    }

    public Integer getMyScore(Long bookId) {
        var loginUser = UserContext.get();
        if (loginUser == null) return null;
        UserBookScore score = userBookScoreMapper.selectOne(
            new LambdaQueryWrapper<UserBookScore>()
                .eq(UserBookScore::getUserId, loginUser.getUserId())
                .eq(UserBookScore::getBookId, bookId));
        return score != null ? score.getScore() : null;
    }

    // ==================== 阅读历史 ====================
    public PageRespDto<ReadHistoryRespDto> getReadHistory(PageReqDto page) {
        Long userId = UserContext.userId();
        IPage<UserReadHistory> result = userReadHistoryMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<UserReadHistory>()
                .eq(UserReadHistory::getUserId, userId)
                .orderByDesc(UserReadHistory::getLastReadTime));
        List<ReadHistoryRespDto> list = result.getRecords().stream().map(h -> {
            ReadHistoryRespDto dto = new ReadHistoryRespDto();
            dto.setId(h.getId()); dto.setUserId(h.getUserId());
            dto.setBookId(h.getBookId()); dto.setLastChapterId(h.getLastChapterId());
            dto.setLastReadTime(h.getLastReadTime());
            BookInfo book = bookInfoMapper.selectById(h.getBookId());
            if (book != null) {
                dto.setBookName(book.getBookName());
                dto.setPicUrl(book.getPicUrl());
                dto.setAuthorName(book.getAuthorName());
            }
            // 补齐最后阅读章节的名称与序号
            if (h.getLastChapterId() != null) {
                BookChapter chapter = bookChapterMapper.selectById(h.getLastChapterId());
                if (chapter != null) {
                    dto.setLastChapterName(chapter.getChapterName());
                    dto.setLastChapterNum(chapter.getChapterNum());
                }
            }
            // 章节可能已被删除：兜底取该书第一章，保证前端可跳转且不出现 undefined
            if (dto.getLastChapterId() == null || dto.getLastChapterName() == null) {
                BookChapter first = bookChapterMapper.selectOne(
                    new LambdaQueryWrapper<BookChapter>()
                        .eq(BookChapter::getBookId, h.getBookId())
                        .orderByAsc(BookChapter::getChapterNum)
                        .last("LIMIT 1"));
                if (first != null) {
                    dto.setLastChapterId(first.getId());
                    dto.setLastChapterName(first.getChapterName());
                    dto.setLastChapterNum(first.getChapterNum());
                }
            }
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public void clearReadHistory() {
        Long userId = UserContext.userId();
        userReadHistoryMapper.delete(
            new LambdaQueryWrapper<UserReadHistory>().eq(UserReadHistory::getUserId, userId));
    }

    // ==================== 私信 ====================
    public PageRespDto<PrivateMessageRespDto> listMessages(Long targetUserId, PageReqDto page) {
        Long currentUserId = UserContext.userId();
        IPage<PrivateMessage> result = privateMessageMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<PrivateMessage>()
                .and(w -> w
                    .and(w1 -> w1.eq(PrivateMessage::getSenderId, currentUserId)
                               .eq(PrivateMessage::getReceiverId, targetUserId))
                    .or(w2 -> w2.eq(PrivateMessage::getSenderId, targetUserId)
                               .eq(PrivateMessage::getReceiverId, currentUserId)))
                .orderByDesc(PrivateMessage::getCreateTime));
        List<PrivateMessageRespDto> list = result.getRecords().stream().map(m -> {
            PrivateMessageRespDto dto = new PrivateMessageRespDto();
            dto.setId(m.getId()); dto.setSenderId(m.getSenderId());
            dto.setReceiverId(m.getReceiverId()); dto.setContent(m.getContent());
            dto.setIsRead(m.getIsRead()); dto.setCreateTime(m.getCreateTime());
            UserInfo sender = userInfoMapper.selectById(m.getSenderId());
            if (sender != null) {
                dto.setSenderName(sender.getNickName() != null ? sender.getNickName() : sender.getUsername());
                dto.setSenderPhoto(sender.getUserPhoto());
            }
            UserInfo receiver = userInfoMapper.selectById(m.getReceiverId());
            if (receiver != null) {
                dto.setReceiverName(receiver.getNickName() != null ? receiver.getNickName() : receiver.getUsername());
            }
            return dto;
        }).collect(Collectors.toList());
        // Mark as read messages from targetUserId to current user
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PrivateMessage> updateWrapper =
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.set(PrivateMessage::getIsRead, CommonConsts.YES)
            .eq(PrivateMessage::getSenderId, targetUserId)
            .eq(PrivateMessage::getReceiverId, currentUserId)
            .eq(PrivateMessage::getIsRead, CommonConsts.NO);
        privateMessageMapper.update(null, updateWrapper);
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public void sendMessage(Long receiverId, MessageSendReqDto dto) {
        Long senderId = UserContext.userId();
        PrivateMessage msg = new PrivateMessage();
        msg.setSenderId(senderId); msg.setReceiverId(receiverId);
        msg.setContent(dto.getContent());
        msg.setIsRead(CommonConsts.NO);
        msg.setCreateTime(LocalDateTime.now());
        privateMessageMapper.insert(msg);
    }

    public UnreadCountRespDto getUnreadCount() {
        Long userId = UserContext.userId();
        Long unread = privateMessageMapper.selectCount(
            new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getReceiverId, userId)
                .eq(PrivateMessage::getIsRead, CommonConsts.NO));
        UnreadCountRespDto dto = new UnreadCountRespDto();
        dto.setUnreadMessages(unread);
        dto.setUnreadReplies(0L);
        return dto;
    }

    public void markAsRead(Long messageId) {
        PrivateMessage msg = privateMessageMapper.selectById(messageId);
        if (msg != null) {
            msg.setIsRead(CommonConsts.YES);
            privateMessageMapper.updateById(msg);
        }
    }

    // ==================== 联系人类表（用于私信界面左侧）====================
    public List<PrivateMessageRespDto> listContacts() {
        Long userId = UserContext.userId();
        List<PrivateMessage> all = privateMessageMapper.selectList(
            new LambdaQueryWrapper<PrivateMessage>()
                .and(w -> w.eq(PrivateMessage::getSenderId, userId)
                         .or().eq(PrivateMessage::getReceiverId, userId))
                .orderByDesc(PrivateMessage::getCreateTime));
        Map<Long, PrivateMessage> latestMap = new LinkedHashMap<>();
        for (PrivateMessage m : all) {
            Long otherId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
            latestMap.putIfAbsent(otherId, m);
        }
        return latestMap.values().stream().map(m -> {
            Long otherId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
            PrivateMessageRespDto dto = new PrivateMessageRespDto();
            dto.setId(m.getId()); dto.setContent(m.getContent());
            dto.setCreateTime(m.getCreateTime()); dto.setIsRead(m.getIsRead());
            dto.setSenderId(m.getSenderId()); dto.setReceiverId(m.getReceiverId());
            UserInfo other = userInfoMapper.selectById(otherId);
            if (other != null) {
                dto.setSenderName(other.getNickName() != null ? other.getNickName() : other.getUsername());
                dto.setSenderPhoto(other.getUserPhoto());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private BookInfoRespDto toBookResp(BookInfo book) {
        if (book == null) return null;
        BookInfoRespDto dto = new BookInfoRespDto();
        dto.setId(book.getId()); dto.setBookName(book.getBookName());
        dto.setAuthorName(book.getAuthorName()); dto.setPicUrl(book.getPicUrl());
        dto.setCategoryName(book.getCategoryName()); dto.setBookDesc(book.getBookDesc());
        dto.setScore(book.getScore()); dto.setVisitCount(book.getVisitCount());
        dto.setWordCount(book.getWordCount()); dto.setIsVip(book.getIsVip());
        dto.setBookStatus(book.getBookStatus()); dto.setAuditStatus(book.getAuditStatus());
        dto.setCreateTime(book.getCreateTime());
        return dto;
    }
}
