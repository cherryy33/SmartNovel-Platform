package com.myproject.novel.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.novel.dao.entity.BookAiIndex;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface BookAiIndexMapper extends BaseMapper<BookAiIndex> {

    /**
     * 首次构建时插入一条索引状态记录（若已存在则跳过）
     */
    @Insert("INSERT INTO book_ai_index (book_id, status) " +
        "SELECT #{bookId}, 0 FROM DUAL " +
        "WHERE NOT EXISTS (SELECT 1 FROM book_ai_index WHERE book_id = #{bookId})")
    int insertIfAbsent(@Param("bookId") Long bookId);

    /**
     * 尝试抢占索引任务：仅当状态为「未构建 0」或「构建失败 3」时置为「构建中 1」
     * @return 受影响行数（1 表示抢占成功，0 表示已有任务在构建）
     */
    @Update("UPDATE book_ai_index SET status = 1, error_msg = NULL " +
        "WHERE book_id = #{bookId} AND status IN (0, 3)")
    int tryLockForIndexing(@Param("bookId") Long bookId);

    /**
     * 重置索引进度（供强制重建前清空状态）
     */
    @Update("UPDATE book_ai_index SET status = 0, indexed_chapter_num = 0, " +
        "indexed_chapter_id = NULL, chunk_count = 0, error_msg = NULL WHERE book_id = #{bookId}")
    int clearProgress(@Param("bookId") Long bookId);

    /**
     * 标记构建完成
     */
    @Update("UPDATE book_ai_index SET status = 2, indexed_chapter_num = #{indexedChapterNum}, " +
        "indexed_chapter_id = #{indexedChapterId}, chunk_count = #{chunkCount}, " +
        "embedding_model = #{embeddingModel}, error_msg = NULL WHERE book_id = #{bookId}")
    int markReady(@Param("bookId") Long bookId,
                  @Param("indexedChapterNum") Integer indexedChapterNum,
                  @Param("indexedChapterId") Long indexedChapterId,
                  @Param("chunkCount") Integer chunkCount,
                  @Param("embeddingModel") String embeddingModel);

    /**
     * 标记构建失败
     */
    @Update("UPDATE book_ai_index SET status = 3, error_msg = #{errorMsg} WHERE book_id = #{bookId}")
    int markFailed(@Param("bookId") Long bookId, @Param("errorMsg") String errorMsg);
}
