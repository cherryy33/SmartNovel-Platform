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
import com.myproject.novel.dao.entity.BookContent;
import com.myproject.novel.dao.entity.BookInfo;
import com.myproject.novel.dao.entity.UserReadHistory;
import com.myproject.novel.dao.mapper.BookChapterMapper;
import com.myproject.novel.dao.mapper.BookContentMapper;
import com.myproject.novel.dao.mapper.BookInfoMapper;
import com.myproject.novel.dao.mapper.UserReadHistoryMapper;
import com.myproject.novel.dto.req.ChapterAddReqDto;
import com.myproject.novel.dto.req.ChapterUpdateReqDto;
import com.myproject.novel.dto.req.ReadingProgressReqDto;
import com.myproject.novel.dto.resp.ChapterContentRespDto;
import com.myproject.novel.dto.resp.ChapterRespDto;
import com.myproject.novel.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 章节服务：阅读、作者增删改、阅读进度
 */
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final BookChapterMapper bookChapterMapper;
    private final BookContentMapper bookContentMapper;
    private final BookInfoMapper bookInfoMapper;
    private final UserReadHistoryMapper userReadHistoryMapper;

    /** 列出某本书的所有章节 */
    public List<ChapterRespDto> listChapters(Long bookId) {
        List<BookChapter> chapters = bookChapterMapper.selectList(
            new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, bookId)
                .orderByAsc(BookChapter::getChapterNum));
        return chapters.stream().map(ch -> {
            ChapterRespDto dto = new ChapterRespDto();
            dto.setId(ch.getId());
            dto.setBookId(ch.getBookId());
            dto.setChapterNum(ch.getChapterNum());
            dto.setChapterName(ch.getChapterName());
            dto.setWordCount(ch.getWordCount());
            dto.setIsVip(ch.getIsVip());
            dto.setCreateTime(ch.getCreateTime());
            dto.setUpdateTime(ch.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    /** 章节详情（含正文），供作家端编辑回显：校验书籍归属后返回章节 + 正文 */
    public ChapterRespDto getChapterDetail(Long bookId, Long chapterId) {
        checkBookOwnership(bookId);
        BookChapter chapter = bookChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getBookId().equals(bookId)) {
            throw new BusinessException(ErrorCode.CHAPTER_NOT_FOUND);
        }
        BookContent content = bookContentMapper.selectOne(
            new LambdaQueryWrapper<BookContent>()
                .eq(BookContent::getChapterId, chapterId));
        ChapterRespDto dto = new ChapterRespDto();
        dto.setId(chapter.getId());
        dto.setBookId(chapter.getBookId());
        dto.setChapterNum(chapter.getChapterNum());
        dto.setChapterName(chapter.getChapterName());
        dto.setWordCount(chapter.getWordCount());
        dto.setIsVip(chapter.getIsVip());
        dto.setCreateTime(chapter.getCreateTime());
        dto.setUpdateTime(chapter.getUpdateTime());
        dto.setContent(content != null && content.getContent() != null ? content.getContent() : "");
        return dto;
    }

    /** 阅读章节（含内容、前后章、VIP校验、阅读记录） */
    public ChapterContentRespDto readChapter(Long bookId, Long chapterId) {
        BookInfo book = checkBookExists(bookId);
        BookChapter chapter = bookChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getBookId().equals(bookId)) {
            throw new BusinessException(ErrorCode.CHAPTER_NOT_FOUND);
        }

        // VIP 校验
        if (chapter.getIsVip() != null && chapter.getIsVip() == CommonConsts.YES) {
            var loginUser = UserContext.require();
            if (loginUser.getIsVip() != null && loginUser.getIsVip() == CommonConsts.YES) {
                // OK
            } else if (CommonConsts.Role.ADMIN.equals(loginUser.getRole())) {
                // OK
            } else {
                throw new BusinessException(ErrorCode.VIP_REQUIRED);
            }
        }

        // 读取内容
        BookContent content = bookContentMapper.selectOne(
            new LambdaQueryWrapper<BookContent>()
                .eq(BookContent::getChapterId, chapterId));
        String chapterContent = content != null ? content.getContent() : "";

        // 前后章
        Long prevId = null, nextId = null;
        List<BookChapter> allChapters = bookChapterMapper.selectList(
            new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, bookId)
                .orderByAsc(BookChapter::getChapterNum));
        for (int i = 0; i < allChapters.size(); i++) {
            if (allChapters.get(i).getId().equals(chapterId)) {
                if (i > 0) prevId = allChapters.get(i - 1).getId();
                if (i < allChapters.size() - 1) nextId = allChapters.get(i + 1).getId();
                break;
            }
        }

        // 记录阅读历史
        var loginUser = UserContext.get();
        if (loginUser != null) {
            saveReadHistory(loginUser.getUserId(), bookId, chapterId);
        }

        // 增加访问量
        book.setVisitCount((book.getVisitCount() == null ? 0L : book.getVisitCount()) + 1);
        bookInfoMapper.updateById(book);

        ChapterContentRespDto dto = new ChapterContentRespDto();
        dto.setId(chapter.getId());
        dto.setBookId(chapter.getBookId());
        dto.setChapterNum(chapter.getChapterNum());
        dto.setChapterName(chapter.getChapterName());
        dto.setWordCount(chapter.getWordCount());
        dto.setIsVip(chapter.getIsVip());
        dto.setContent(chapterContent);
        dto.setPrevChapterId(prevId);
        dto.setNextChapterId(nextId);
        dto.setTotalChapters(allChapters.size());
        return dto;
    }

    /** 保存阅读进度 */
    public void saveReadingProgress(Long bookId, ReadingProgressReqDto dto) {
        Long userId = UserContext.userId();
        saveReadHistory(userId, bookId, dto.getLastChapterId());
    }

    // ---- 作者侧 ----

    /** 新增章节 */
    @Transactional
    public void addChapter(Long bookId, ChapterAddReqDto dto) {
        checkBookOwnership(bookId);
        BookInfo book = bookInfoMapper.selectById(bookId);

        // 计算章节号
        List<BookChapter> existing = bookChapterMapper.selectList(
            new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, bookId)
                .orderByDesc(BookChapter::getChapterNum)
                .last("limit 1"));
        int nextNum = existing.isEmpty() ? 1 : existing.get(0).getChapterNum() + 1;

        int wordCount = dto.getContent() != null ? dto.getContent().length() : 0;

        BookChapter chapter = new BookChapter();
        chapter.setBookId(bookId);
        chapter.setChapterNum(nextNum);
        chapter.setChapterName(dto.getChapterName());
        chapter.setWordCount(wordCount);
        chapter.setIsVip(dto.getIsVip() != null ? dto.getIsVip() : CommonConsts.NO);
        chapter.setCreateTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());
        bookChapterMapper.insert(chapter);

        BookContent content = new BookContent();
        content.setChapterId(chapter.getId());
        content.setContent(dto.getContent());
        content.setCreateTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        bookContentMapper.insert(content);

        // 更新书的总字数
        book.setWordCount((book.getWordCount() != null ? book.getWordCount() : 0) + wordCount);
        book.setLastChapterId(chapter.getId());
        book.setLastChapterName(chapter.getChapterName());
        book.setLastChapterUpdateTime(LocalDateTime.now());
        bookInfoMapper.updateById(book);
    }

    /** 编辑章节 */
    @Transactional
    public void updateChapter(Long bookId, Long chapterId, ChapterUpdateReqDto dto) {
        checkBookOwnership(bookId);
        BookChapter chapter = bookChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getBookId().equals(bookId)) {
            throw new BusinessException(ErrorCode.CHAPTER_NOT_FOUND);
        }
        if (dto.getChapterName() != null) chapter.setChapterName(dto.getChapterName());
        if (dto.getIsVip() != null) chapter.setIsVip(dto.getIsVip());
        chapter.setUpdateTime(LocalDateTime.now());

        BookContent content = bookContentMapper.selectOne(
            new LambdaQueryWrapper<BookContent>().eq(BookContent::getChapterId, chapterId));
        if (dto.getContent() != null) {
            if (content != null) {
                content.setContent(dto.getContent());
                content.setUpdateTime(LocalDateTime.now());
                bookContentMapper.updateById(content);
            } else {
                content = new BookContent();
                content.setChapterId(chapterId);
                content.setContent(dto.getContent());
                content.setCreateTime(LocalDateTime.now());
                content.setUpdateTime(LocalDateTime.now());
                bookContentMapper.insert(content);
            }
            int newWordCount = dto.getContent().length();
            // 同步维护书籍总字数：差值 = 新字数 - 旧字数
            int delta = newWordCount - (chapter.getWordCount() != null ? chapter.getWordCount() : 0);
            chapter.setWordCount(newWordCount);
            BookInfo book = bookInfoMapper.selectById(bookId);
            if (book != null) {
                book.setWordCount(Math.max(0, (book.getWordCount() != null ? book.getWordCount() : 0) + delta));
                bookInfoMapper.updateById(book);
            }
        }
        bookChapterMapper.updateById(chapter);

        // 若编辑的是最新章节，同步书籍的「最新章节」快照
        BookInfo latest = bookInfoMapper.selectById(bookId);
        if (latest != null && chapterId.equals(latest.getLastChapterId())) {
            latest.setLastChapterName(chapter.getChapterName());
            latest.setLastChapterUpdateTime(LocalDateTime.now());
            bookInfoMapper.updateById(latest);
        }
    }

    /** 删除章节 */
    @Transactional
    public void deleteChapter(Long bookId, Long chapterId) {
        checkBookOwnership(bookId);
        BookChapter chapter = bookChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getBookId().equals(bookId)) {
            throw new BusinessException(ErrorCode.CHAPTER_NOT_FOUND);
        }
        bookContentMapper.delete(new LambdaQueryWrapper<BookContent>()
            .eq(BookContent::getChapterId, chapterId));
        bookChapterMapper.deleteById(chapterId);

        // 更新书的字数
        BookInfo book = bookInfoMapper.selectById(bookId);
        book.setWordCount(Math.max(0, (book.getWordCount() != null ? book.getWordCount() : 0) - (chapter.getWordCount() != null ? chapter.getWordCount() : 0)));
        bookInfoMapper.updateById(book);
    }

    // ---- 管理员侧 ----
    public PageRespDto<ChapterRespDto> adminListChapters(Long bookId, PageReqDto page) {
        IPage<BookChapter> result = bookChapterMapper.selectPage(
            new Page<>(page.getPageNum(), page.getPageSize()),
            new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, bookId)
                .orderByAsc(BookChapter::getChapterNum));
        List<ChapterRespDto> list = result.getRecords().stream().map(ch -> {
            ChapterRespDto dto = new ChapterRespDto();
            dto.setId(ch.getId()); dto.setBookId(ch.getBookId());
            dto.setChapterNum(ch.getChapterNum()); dto.setChapterName(ch.getChapterName());
            dto.setWordCount(ch.getWordCount()); dto.setIsVip(ch.getIsVip());
            dto.setCreateTime(ch.getCreateTime()); dto.setUpdateTime(ch.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
        return PageRespDto.of(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public void adminDeleteChapter(Long bookId, Long chapterId) {
        BookChapter chapter = bookChapterMapper.selectById(chapterId);
        if (chapter == null || !chapter.getBookId().equals(bookId)) {
            throw new BusinessException(ErrorCode.CHAPTER_NOT_FOUND);
        }
        bookContentMapper.delete(new LambdaQueryWrapper<BookContent>().eq(BookContent::getChapterId, chapterId));
        bookChapterMapper.deleteById(chapterId);
    }

    private void saveReadHistory(Long userId, Long bookId, Long chapterId) {
        UserReadHistory history = userReadHistoryMapper.selectOne(
            new LambdaQueryWrapper<UserReadHistory>()
                .eq(UserReadHistory::getUserId, userId)
                .eq(UserReadHistory::getBookId, bookId));
        if (history == null) {
            history = new UserReadHistory();
            history.setUserId(userId);
            history.setBookId(bookId);
            history.setLastChapterId(chapterId);
            history.setLastReadTime(LocalDateTime.now());
            history.setCreateTime(LocalDateTime.now());
            history.setUpdateTime(LocalDateTime.now());
            userReadHistoryMapper.insert(history);
        } else {
            history.setLastChapterId(chapterId);
            history.setLastReadTime(LocalDateTime.now());
            history.setUpdateTime(LocalDateTime.now());
            userReadHistoryMapper.updateById(history);
        }
    }

    private BookInfo checkBookExists(Long bookId) {
        BookInfo book = bookInfoMapper.selectById(bookId);
        if (book == null) throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        return book;
    }

    private void checkBookOwnership(Long bookId) {
        Long authorId = UserContext.require().getAuthorId();
        BookInfo book = bookInfoMapper.selectById(bookId);
        if (book == null) throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        if (!authorId.equals(book.getAuthorId()))
            throw new BusinessException(ErrorCode.USER_NO_PERMISSION);
    }
}
