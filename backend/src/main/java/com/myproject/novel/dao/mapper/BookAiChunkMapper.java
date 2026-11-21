package com.myproject.novel.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.novel.dao.entity.BookAiChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface BookAiChunkMapper extends BaseMapper<BookAiChunk> {

    /**
     * 删除某本小说的全部分块记录（强制重建前清理）
     */
    @Delete("DELETE FROM book_ai_chunk WHERE book_id = #{bookId}")
    int deleteByBookId(@Param("bookId") Long bookId);
}
