package com.myproject.novel.manager.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.common.constant.AiConsts;
import com.myproject.novel.common.util.ChapterChunker;
import com.myproject.novel.config.AiRagProperties;
import com.myproject.novel.dao.entity.BookAiChunk;
import com.myproject.novel.dao.entity.BookAiIndex;
import com.myproject.novel.dao.entity.BookChapter;
import com.myproject.novel.dao.entity.BookContent;
import com.myproject.novel.dao.entity.BookInfo;
import com.myproject.novel.dao.mapper.BookAiChunkMapper;
import com.myproject.novel.dao.mapper.BookAiIndexMapper;
import com.myproject.novel.dao.mapper.BookChapterMapper;
import com.myproject.novel.dao.mapper.BookContentMapper;
import com.myproject.novel.dao.mapper.BookInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小说 RAG 向量索引管理类
 * <p>
 * 负责把「小说章节正文」按章节增量地分块、向量化并写入 Spring AI 的 {@link SimpleVectorStore}，
 * 同时在 {@code book_ai_chunk} 表中保留分块原文，用于引用溯源。
 * <p>
 * 索引任务通过 {@link Async} 在虚拟线程中执行，读者进入阅读页时触发一次即可，
 * 后续阅读进度推进时只增量索引「尚未索引的新章节」。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookRagIndexManager {

    private final BookInfoMapper bookInfoMapper;
    private final BookChapterMapper bookChapterMapper;
    private final BookContentMapper bookContentMapper;
    private final BookAiIndexMapper bookAiIndexMapper;
    private final BookAiChunkMapper bookAiChunkMapper;
    private final SimpleVectorStore vectorStore;
    private final AiRagProperties aiRagProperties;

    /** 向量库文件读写锁：SimpleVectorStore 的持久化方法不支持并发写 */
    private final Object vectorStoreFileLock = new Object();

    /**
     * 异步构建/增量更新某本小说的知识库
     *
     * @param bookId         小说 ID
     * @param upToChapterNum 本次索引到第几章；为空时索引全书
     * @param force          true-清空已有索引并全量重建，false-增量构建
     */
    @Async
    public void indexBookAsync(Long bookId, Integer upToChapterNum, boolean force) {
        BookInfo bookInfo = bookInfoMapper.selectById(bookId);
        if (bookInfo == null) {
            log.warn("小说[{}]不存在，跳过 AI 知识库构建", bookId);
            return;
        }

        bookAiIndexMapper.insertIfAbsent(bookId);
        if (bookAiIndexMapper.tryLockForIndexing(bookId) == 0) {
            log.info("小说[{}]的 AI 知识库正在构建中，忽略本次触发", bookId);
            return;
        }

        try {
            if (force) {
                clearIndexData(bookId);
                bookAiIndexMapper.clearProgress(bookId);
            }

            BookAiIndex state = selectIndex(bookId);
            boolean neverIndexed = state == null || state.getIndexedChapterId() == null;
            int startChapterNum = neverIndexed ? -1 : state.getIndexedChapterNum();
            int startChunkCount = state == null || state.getChunkCount() == null
                ? 0 : state.getChunkCount();
            Long startChapterId = state == null ? null : state.getIndexedChapterId();

            int targetChapterNum = resolveTargetChapterNum(bookId, upToChapterNum);
            if (targetChapterNum <= startChapterNum) {
                bookAiIndexMapper.markReady(bookId, startChapterNum, startChapterId, startChunkCount,
                    aiRagProperties.getEmbeddingModel());
                log.info("小说[{}]的知识库已覆盖到第{}章，无需重复构建", bookId, startChapterNum);
                return;
            }

            List<BookChapter> chapters = bookChapterMapper.selectList(
                new LambdaQueryWrapper<BookChapter>()
                    .eq(BookChapter::getBookId, bookId)
                    .gt(BookChapter::getChapterNum, startChapterNum)
                    .le(BookChapter::getChapterNum, targetChapterNum)
                    .orderByAsc(BookChapter::getChapterNum)
                    .last("limit " + aiRagProperties.getMaxChaptersPerIndex()));
            if (chapters.isEmpty()) {
                bookAiIndexMapper.markReady(bookId, Math.max(startChapterNum, 0), startChapterId,
                    startChunkCount, aiRagProperties.getEmbeddingModel());
                return;
            }

            Map<Long, String> contentMap = loadChapterContents(chapters);

            int totalChunks = startChunkCount;
            int lastChapterNum = startChapterNum;
            Long lastChapterId = startChapterId;

            for (BookChapter chapter : chapters) {
                lastChapterNum = chapter.getChapterNum();
                lastChapterId = chapter.getId();

                String content = contentMap.get(chapter.getId());
                if (content == null || content.isBlank()) {
                    continue;
                }

                List<String> pieces = ChapterChunker.chunk(content,
                    aiRagProperties.getChunkSize(), aiRagProperties.getChunkOverlap());
                if (pieces.isEmpty()) {
                    continue;
                }

                List<Document> documents = new ArrayList<>(pieces.size());
                for (int i = 0; i < pieces.size(); i++) {
                    String text = pieces.get(i);
                    String vectorId = buildVectorId(bookId, chapter.getId(), i);

                    Map<String, Object> metadata = new HashMap<>(8);
                    metadata.put(AiConsts.RagMetadata.BOOK_ID, String.valueOf(bookId));
                    metadata.put(AiConsts.RagMetadata.CHAPTER_ID, String.valueOf(chapter.getId()));
                    metadata.put(AiConsts.RagMetadata.CHAPTER_NUM, chapter.getChapterNum());
                    metadata.put(AiConsts.RagMetadata.CHAPTER_NAME, chapter.getChapterName());
                    metadata.put(AiConsts.RagMetadata.CHUNK_INDEX, i);
                    documents.add(new Document(vectorId, text, metadata));

                    BookAiChunk chunk = new BookAiChunk();
                    chunk.setBookId(bookId);
                    chunk.setChapterId(chapter.getId());
                    chunk.setChapterNum(chapter.getChapterNum());
                    chunk.setChapterName(chapter.getChapterName());
                    chunk.setChunkIndex(i);
                    chunk.setContent(text);
                    chunk.setCharCount(text.length());
                    chunk.setVectorId(vectorId);
                    chunk.setCreateTime(LocalDateTime.now());
                    bookAiChunkMapper.insert(chunk);
                }

                vectorStore.add(documents);
                totalChunks += documents.size();
                log.info("小说[{}]第{}章《{}》向量化完成，分块数={}",
                    bookId, chapter.getChapterNum(), chapter.getChapterName(), documents.size());
            }

            persistVectorStore();
            bookAiIndexMapper.markReady(bookId, lastChapterNum, lastChapterId, totalChunks,
                aiRagProperties.getEmbeddingModel());
            log.info("小说[{}]AI 知识库构建完成：已索引到第{}章，共{}个分块",
                bookId, lastChapterNum, totalChunks);
        } catch (Exception e) {
            log.error("小说[{}]AI 知识库构建失败", bookId, e);
            try {
                clearIndexData(bookId);
                bookAiIndexMapper.clearProgress(bookId);
            } catch (Exception cleanupException) {
                log.error("清理小说[{}]的索引数据失败", bookId, cleanupException);
            }
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            bookAiIndexMapper.markFailed(bookId,
                message.length() > 480 ? message.substring(0, 480) : message);
        }
    }

    /**
     * 删除某本小说已建立的索引（向量库文档 + 分块记录）
     */
    private void clearIndexData(Long bookId) {
        List<BookAiChunk> chunks = bookAiChunkMapper.selectList(
            new LambdaQueryWrapper<BookAiChunk>()
                .select(BookAiChunk::getVectorId)
                .eq(BookAiChunk::getBookId, bookId));
        List<String> vectorIds = chunks.stream()
            .map(BookAiChunk::getVectorId)
            .filter(vectorId -> vectorId != null)
            .toList();
        if (!vectorIds.isEmpty()) {
            vectorStore.delete(vectorIds);
        }
        bookAiChunkMapper.delete(new LambdaQueryWrapper<BookAiChunk>()
            .eq(BookAiChunk::getBookId, bookId));
        persistVectorStore();
    }

    private BookAiIndex selectIndex(Long bookId) {
        return bookAiIndexMapper.selectOne(new LambdaQueryWrapper<BookAiIndex>()
            .eq(BookAiIndex::getBookId, bookId)
            .last("limit 1"));
    }

    private int resolveTargetChapterNum(Long bookId, Integer upToChapterNum) {
        if (upToChapterNum != null && upToChapterNum > 0) {
            return upToChapterNum;
        }
        List<BookChapter> chapters = bookChapterMapper.selectList(new LambdaQueryWrapper<BookChapter>()
            .eq(BookChapter::getBookId, bookId)
            .orderByDesc(BookChapter::getChapterNum)
            .last("limit 1"));
        return chapters.isEmpty() ? 0 : chapters.get(0).getChapterNum();
    }

    private Map<Long, String> loadChapterContents(List<BookChapter> chapters) {
        List<Long> chapterIds = chapters.stream().map(BookChapter::getId).toList();
        List<BookContent> contents = bookContentMapper.selectList(
            new LambdaQueryWrapper<BookContent>()
                .select(BookContent::getChapterId, BookContent::getContent)
                .in(BookContent::getChapterId, chapterIds));
        Map<Long, String> contentMap = new HashMap<>(contents.size());
        for (BookContent content : contents) {
            contentMap.put(content.getChapterId(), content.getContent());
        }
        return contentMap;
    }

    private String buildVectorId(Long bookId, Long chapterId, int chunkIndex) {
        return "book-" + bookId + "-chapter-" + chapterId + "-chunk-" + chunkIndex;
    }

    /**
     * 持久化向量库到本地 JSON 文件。
     * 持久化失败不会影响本次构建（向量仍在内存中可用），只是重启后需要重新构建。
     */
    private void persistVectorStore() {
        File file = new File(aiRagProperties.getVectorStorePath());
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("无法创建向量库目录：{}", parent.getAbsolutePath());
            return;
        }
        synchronized (vectorStoreFileLock) {
            try {
                vectorStore.save(file);
                log.info("向量库已持久化到 {}", file.getAbsolutePath());
            } catch (Exception e) {
                log.error("向量库持久化失败：{}", file.getAbsolutePath(), e);
            }
        }
    }
}
