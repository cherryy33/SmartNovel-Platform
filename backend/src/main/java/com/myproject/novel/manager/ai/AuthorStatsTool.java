package com.myproject.novel.manager.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myproject.novel.dao.entity.BookChapter;
import com.myproject.novel.dao.entity.BookInfo;
import com.myproject.novel.dao.mapper.BookChapterMapper;
import com.myproject.novel.dao.mapper.BookInfoMapper;
import com.myproject.novel.dto.resp.CharacterAppearanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Objects;

/**
 * 作者后台 AI 助手可调用的工具集（Tool Calling）
 * <p>
 * 每次请求创建一个实例，绑定当前登录作者；工具内部再次校验小说归属，
 * 避免大模型被诱导查询其他作者的数据。
 */
@RequiredArgsConstructor
public class AuthorStatsTool {

    private final Long authorId;
    private final BookInfoMapper bookInfoMapper;
    private final BookChapterMapper bookChapterMapper;

    /**
     * 统计某个角色在指定小说中的出场次数
     */
    @Tool(description = "统计指定小说中某个角色（姓名或称呼）出现的总次数，以及出场过的章节列表。"
        + "当作者询问角色出场次数、活跃章节、某角色在哪些章节出现等问题时调用此工具。")
    public String countCharacterAppearances(
        @ToolParam(description = "小说ID，必须是当前作者自己的作品") Long bookId,
        @ToolParam(description = "角色名称或称呼，例如：萧炎") String characterName) {
        BookInfo bookInfo = bookInfoMapper.selectById(bookId);
        if (bookInfo == null) {
            return "未找到 ID 为 " + bookId + " 的小说。";
        }
        if (!Objects.equals(bookInfo.getAuthorId(), authorId)) {
            return "无权查询该小说：它不属于当前作者。";
        }
        if (characterName == null || characterName.isBlank()) {
            return "角色名称不能为空。";
        }
        String name = characterName.strip();
        if (name.length() > 20) {
            return "角色名称过长，请提供 20 个字符以内的姓名或称呼。";
        }

        List<CharacterAppearanceDto> appearances = bookChapterMapper
            .countCharacterAppearances(bookId, name);
        if (appearances.isEmpty()) {
            return "在《" + bookInfo.getBookName() + "》中没有检索到角色「" + name
                + "」的出场记录，可能是姓名写法不同或该角色尚未出场。";
        }

        long total = appearances.stream()
            .mapToLong(item -> item.getAppearCount() == null ? 0 : item.getAppearCount())
            .sum();
        StringBuilder result = new StringBuilder();
        result.append("角色「").append(name).append("」在《").append(bookInfo.getBookName())
            .append("》中共出现 ").append(total).append(" 次，分布在 ")
            .append(appearances.size()).append(" 个章节：\n");
        appearances.stream().limit(10).forEach(item -> result.append("- 第")
            .append(item.getChapterNum() + 1).append("章《").append(item.getChapterName()).append("》：")
            .append(item.getAppearCount()).append(" 次\n"));
        if (appearances.size() > 10) {
            result.append("（此处只列出前 10 个章节）\n");
        }
        return result.toString();
    }

    /**
     * 查询某本作品的基础统计信息
     */
    @Tool(description = "查询当前作者某本小说的基础统计信息：书名、章节数、总字数、点击量、评论数。"
        + "当作者询问作品数据、码字字数、章节数量等问题时调用此工具。")
    public String getBookOverview(
        @ToolParam(description = "小说ID，必须是当前作者自己的作品") Long bookId) {
        BookInfo bookInfo = bookInfoMapper.selectById(bookId);
        if (bookInfo == null) {
            return "未找到 ID 为 " + bookId + " 的小说。";
        }
        if (!Objects.equals(bookInfo.getAuthorId(), authorId)) {
            return "无权查询该小说：它不属于当前作者。";
        }
        Long chapterCount = bookChapterMapper.selectCount(new LambdaQueryWrapper<BookChapter>()
            .eq(BookChapter::getBookId, bookId));
        return "《" + bookInfo.getBookName() + "》统计信息：\n"
            + "- 章节数：" + chapterCount + "\n"
            + "- 总字数：" + bookInfo.getWordCount() + "\n"
            + "- 点击量：" + bookInfo.getVisitCount() + "\n"
            + "- 评论数：" + bookInfo.getCommentCount() + "\n";
    }
}
