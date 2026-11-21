package com.myproject.novel.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myproject.novel.dao.entity.BookChapter;
import com.myproject.novel.dto.resp.CharacterAppearanceDto;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BookChapterMapper extends BaseMapper<BookChapter> {

    /**
     * 统计角色在各章节中的出现次数：用「替换前后的字符长度差」计算非重叠出现次数
     */
    @Select("SELECT c.chapter_num AS chapterNum, c.chapter_name AS chapterName, " +
        "(CHAR_LENGTH(bc.content) - CHAR_LENGTH(REPLACE(bc.content, #{name}, ''))) / CHAR_LENGTH(#{name}) AS appearCount " +
        "FROM book_content bc INNER JOIN book_chapter c ON bc.chapter_id = c.id " +
        "WHERE c.book_id = #{bookId} AND bc.content LIKE CONCAT('%', #{name}, '%') " +
        "ORDER BY c.chapter_num ASC")
    List<CharacterAppearanceDto> countCharacterAppearances(@Param("bookId") Long bookId,
                                                           @Param("name") String name);
}
