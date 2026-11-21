package com.myproject.novel.service;

import com.myproject.novel.common.constant.AiConsts;
import com.myproject.novel.config.AiRagProperties;
import com.myproject.novel.dao.entity.BookAiIndex;
import com.myproject.novel.dao.entity.BookAiChunk;
import com.myproject.novel.dao.entity.BookChapter;
import com.myproject.novel.dao.entity.BookContent;
import com.myproject.novel.dao.entity.BookInfo;
import com.myproject.novel.dao.mapper.BookAiIndexMapper;
import com.myproject.novel.dao.mapper.BookAiChunkMapper;
import com.myproject.novel.dao.mapper.BookChapterMapper;
import com.myproject.novel.dao.mapper.BookContentMapper;
import com.myproject.novel.dao.mapper.BookInfoMapper;
import com.myproject.novel.dto.req.*;
import com.myproject.novel.dto.resp.*;
import com.myproject.novel.manager.ai.AuthorStatsTool;
import com.myproject.novel.manager.ai.BookRagIndexManager;
import com.myproject.novel.security.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AI 服务：阅读助手 / 作者工具调用 / 管理员数据分析 / 写作助手
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ChatClient chatClient;
    private final SimpleVectorStore vectorStore;
    private final AiRagProperties aiRagProperties;
    private final BookRagIndexManager bookRagIndexManager;
    private final BookInfoMapper bookInfoMapper;
    private final BookAiIndexMapper bookAiIndexMapper;
    private final BookAiChunkMapper bookAiChunkMapper;
    private final BookChapterMapper bookChapterMapper;
    private final BookContentMapper bookContentMapper;

    // ==================== 读者 AI 阅读助手（RAG） ====================

    public AiToolRespDto chatWithBook(Long bookId, BookAiChatReqDto dto) {
        BookInfo book = bookInfoMapper.selectById(bookId);
        if (book == null) return answer("未找到该小说。");

        // 检查索引状态，未构建则触发异步构建，同时用章节正文直接回答
        BookAiIndex indexState = bookAiIndexMapper.selectById(bookId);
        if (indexState == null || indexState.getStatus() != AiConsts.IndexStatus.READY) {
            bookRagIndexManager.indexBookAsync(bookId, null, false);
            return fallbackAnswerWithChapters(book, dto.getQuestion());
        }

        // 向量检索
        SearchRequest searchRequest = SearchRequest.builder()
            .query(dto.getQuestion())
            .topK(aiRagProperties.getTopK())
            .similarityThreshold(aiRagProperties.getSimilarityThreshold())
            .filterExpression("bookId == '" + bookId + "'")
            .build();
        List<Document> docs = vectorStore.similaritySearch(searchRequest);

        String context = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n"));
        if (context.length() > aiRagProperties.getContextMaxChars()) {
            context = context.substring(0, aiRagProperties.getContextMaxChars());
        }

        // 构建来源引用
        List<BookAiSourceDto> sources = docs.stream().map(doc -> {
            BookAiSourceDto s = new BookAiSourceDto();
            s.setChapterName((String) doc.getMetadata().get(AiConsts.RagMetadata.CHAPTER_NAME));
            s.setChapterNum(doc.getMetadata().get(AiConsts.RagMetadata.CHAPTER_NUM) != null
                ? Integer.parseInt(String.valueOf(doc.getMetadata().get(AiConsts.RagMetadata.CHAPTER_NUM))) : null);
            s.setChapterId(doc.getMetadata().get(AiConsts.RagMetadata.CHAPTER_ID) != null
                ? Long.parseLong(String.valueOf(doc.getMetadata().get(AiConsts.RagMetadata.CHAPTER_ID))) : null);
            return s;
        }).collect(Collectors.toList());

        String prompt = AiConsts.RAG_SYSTEM_PROMPT + "\n\n【参考章节内容】\n" + context
            + "\n\n读者问题：" + dto.getQuestion();
        String answer = chatClient.prompt().user(prompt).call().content();

        AiToolRespDto resp = new AiToolRespDto();
        resp.setAnswer(answer);
        return resp;
    }

    // ==================== 作者 AI 助手（Tool Calling） ====================

    public AiToolRespDto authorAiAsk(AuthorAiAskReqDto dto) {
        Long authorId = UserContext.require().getAuthorId();
        AuthorStatsTool tool = new AuthorStatsTool(authorId, bookInfoMapper, null);
        // Simplified: direct chat + tool call
        String answer = chatClient.prompt()
            .system(AiConsts.AUTHOR_TOOL_SYSTEM_PROMPT)
            .user(dto.getQuestion())
            .tools(tool)
            .call().content();
        AiToolRespDto resp = new AiToolRespDto();
        resp.setAnswer(answer);
        return resp;
    }

    // ==================== 管理员 AI 数据分析 ====================

    public AiToolRespDto adminAiAsk(AdminAiAskReqDto dto) {
        String answer = chatClient.prompt()
            .system(AiConsts.ADMIN_TOOL_SYSTEM_PROMPT)
            .user(dto.getQuestion())
            .call().content();
        AiToolRespDto resp = new AiToolRespDto();
        resp.setAnswer(answer);
        return resp;
    }

    // ==================== AI 写作助手 ====================

    public AiToolRespDto writeAssist(Long bookId, AiExpandReqDto dto) {
        String actionType = resolveActionType(dto.getActionType());
        String content = normalizeContent(dto.getContent());
        String extra = extraRequirement(dto.getPrompt());

        String processed;
        try {
            processed = chatClient.prompt()
                .system(WRITE_SYSTEM_PROMPT)
                .user(actionPrompt(actionType) + extra + content)
                .call().content();
        } catch (Exception e) {
            // 外部大模型不可用时，走本地情境续写兜底，保证接口不报错
            log.warn("AI 写作助手调用失败，使用本地兜底: {}", e.getMessage());
            processed = localWriteFallback(content, actionType);
        }

        AiToolRespDto resp = new AiToolRespDto();
        resp.setProcessedContent(processed);
        resp.setAnswer(processed);
        return resp;
    }

    /**
     * 流式写作助手：边生成边回调，前端可实时展示内容。
     * 大模型不可用时同样走本地兜底，并通过回调一次性发送。
     *
     * @param onChunk 每收到一段增量文本时调用
     */
    public void writeAssistStream(Long bookId, AiExpandReqDto dto,
            Consumer<String> onChunk) {
        String actionType = resolveActionType(dto.getActionType());
        String content = normalizeContent(dto.getContent());
        String extra = extraRequirement(dto.getPrompt());

        try {
            // ChatClient.stream() 返回 Flux<String>，逐块推送增量文本
            chatClient.prompt()
                .system(WRITE_SYSTEM_PROMPT)
                .user(actionPrompt(actionType) + extra + content)
                .stream().content()
                .toIterable()
                .forEach(chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        onChunk.accept(chunk);
                    }
                });
        } catch (Exception e) {
            // 客户端主动断开（关闭弹窗/停止生成）：直接结束，不再走兜底
            if (e instanceof java.io.UncheckedIOException
                    || e instanceof org.springframework.web.context.request.async.AsyncRequestNotUsableException) {
                throw new java.io.UncheckedIOException(e.getMessage(), new java.io.IOException(e));
            }
            log.warn("AI 写作助手流式调用失败，使用本地兜底: {}", e.getMessage());
            onChunk.accept(localWriteFallback(content, actionType));
        }
    }

    private static final String WRITE_SYSTEM_PROMPT =
        "你是一位专业的小说创作助手，擅长扩写、缩写、续写和润色小说片段。";

    /** 操作类型归一化，缺省按「扩写」处理 */
    private String resolveActionType(String raw) {
        return raw == null || raw.isBlank() ? "expand" : raw.trim().toLowerCase();
    }

    /** 控制送入大模型的正文长度，避免超长章节超出上下文 */
    private String normalizeContent(String raw) {
        String content = raw == null ? "" : raw;
        int maxLen = 8000;
        return content.length() > maxLen ? content.substring(0, maxLen) : content;
    }

    private String extraRequirement(String prompt) {
        return prompt != null && !prompt.isBlank()
                ? "\n补充要求：" + prompt.trim() + "\n" : "";
    }

    private String actionPrompt(String actionType) {
        return switch (actionType) {
            case "expand" -> "请将以下小说片段进行扩写，增加细节描写、心理活动和环境渲染，保持原有风格和剧情走向：\n";
            case "shrink" -> "请将以下小说片段进行缩写，保留关键情节和对话，去除冗余描述：\n";
            case "continue" -> "请根据以下小说片段的情节和风格，续写接下来的内容（约300-500字）：\n";
            case "polish" -> "请润色以下小说片段，优化文笔、修正语病、增强表现力，但不要改变原意：\n";
            default -> "请将以下小说片段进行扩写，增加细节描写、心理活动和环境渲染，保持原有风格和剧情走向：\n";
        };
    }

    /** 大模型不可用时的本地写作兜底：按操作类型做简单的情境延展 */
    private String localWriteFallback(String content, String actionType) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            text = "（尚未输入正文）";
        }
        String tail = text.length() > 40 ? text.substring(text.length() - 40) : text;
        return switch (actionType) {
            case "shrink" -> text.length() <= 120 ? text : text.substring(0, 120) + "……";
            case "polish" -> text.replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\n\n").trim();
            case "continue" -> text + "\n\n" + "风从远处吹来，将未完的故事继续推向深处。"
                    + "人物的呼吸渐渐平稳，而新的悬念也在这一刻悄然浮现……";
            default -> text + "\n\n" + "夜色缓缓铺开，空气里仿佛多了几分温度。"
                    + "细微的声响在四周回荡，光影交错之间，情绪一层层推进。"
                    + "他想起方才那句话——「" + tail + "」——心中忽然有了决断。";
        };
    }

    // ==================== 知识库未就绪时的兜底回答 ====================

    /**
     * 知识库未就绪时，用章节正文作为上下文直接回答读者问题
     */
    private AiToolRespDto fallbackAnswerWithChapters(BookInfo book, String question) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("【小说信息】\n书名：").append(book.getBookName()).append("\n");
        if (book.getBookDesc() != null && !book.getBookDesc().isEmpty()) {
            ctx.append("简介：").append(book.getBookDesc()).append("\n");
        }

        // 获取第一章的标题和正文作为上下文
        List<BookChapter> firstChapter = bookChapterMapper.selectList(
            new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, book.getId())
                .orderByAsc(BookChapter::getChapterNum)
                .last("LIMIT 1"));
        if (!firstChapter.isEmpty()) {
            BookChapter ch = firstChapter.get(0);
            ctx.append("\n首章标题：").append(displayChapterName(ch)).append("\n");
            BookContent content = bookContentMapper.selectById(ch.getId());
            if (content != null && content.getContent() != null) {
                String text = content.getContent();
                int maxLen = 3000;
                ctx.append("首章正文：").append(text.length() > maxLen ? text.substring(0, maxLen) + "\n...(共" + text.length() + "字)" : text).append("\n");
            }
        }

        // 目录列表
        List<BookChapter> allChapters = bookChapterMapper.selectList(
            new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, book.getId())
                .orderByAsc(BookChapter::getChapterNum));
        if (!allChapters.isEmpty()) {
            ctx.append("\n章节目录（共").append(allChapters.size()).append("章）：\n");
            for (BookChapter ch : allChapters) {
                ctx.append(displayChapterName(ch)).append("\n");
            }
        }

        // 尝试调用大模型
        try {
            String system = "你是小说阅读助手。基于提供的小说章节内容回答读者问题。";
            String prompt = ctx.toString() + "\n\n读者问题：" + question;
            String aiAnswer = chatClient.prompt().system(system).user(prompt).call().content();
            return answer(aiAnswer);
        } catch (Exception e) {
            log.warn("ChatClient 不可用，使用智能摘要兜底: {}", e.getMessage());
            return smartSummaryFallback(book, ctx.toString(), question);
        }
    }

    /**
     * 无需 API Key 的纯文本智能摘要兜底
     */
    private AiToolRespDto smartSummaryFallback(BookInfo book, String context, String question) {
        String q = question != null ? question : "";
        StringBuilder reply = new StringBuilder();

        // 提取章节目录段落
        String tocPart = "";
        int tocIdx = context.indexOf("章节目录");
        if (tocIdx >= 0) tocPart = context.substring(tocIdx);

        // 提取首章信息
        String firstChapterPart = "";
        int firstIdx = context.indexOf("首章标题");
        if (firstIdx >= 0) {
            int firstEnd = tocIdx >= 0 ? tocIdx : context.length();
            firstChapterPart = context.substring(firstIdx, firstEnd);
        }

        if (q.contains("主要内容") || q.contains("讲了什么") || q.contains("简介") || q.contains("概括") || q.contains("总结") || q.contains("讲什么")) {
            reply.append("📖 《").append(book.getBookName()).append("》\n\n");
            if (book.getBookDesc() != null && !book.getBookDesc().isEmpty()) {
                reply.append(book.getBookDesc()).append("\n\n");
            }
            reply.append(firstChapterPart.isEmpty() ? "本书包含多个精彩章节。" : firstChapterPart);
            reply.append("\n\n💡 AI 知识库正在后台构建中，完成后可进行更深度的逐章分析。欢迎先阅读已加载的首章内容。");
        } else if (q.contains("章节") || q.contains("目录") || q.contains("多少章")) {
            reply.append("📚 《").append(book.getBookName()).append("》章节一览：\n\n");
            reply.append(tocPart.isEmpty() ? "暂未获取到章节目录。" : tocPart.replace("章节目录（", "").replace("）：", "："));
        } else if (q.contains("作者") || q.contains("谁写的")) {
            reply.append("✍️ 《").append(book.getBookName()).append("》的作者信息可在作品详情页查看。\n\n");
            reply.append("目前可为您提供：\n- 作品简介与章节目录\n- 首章核心情节概要\n- AI 知识库就绪后还可对任意章节进行深度提问");
        } else {
            reply.append("🤖 关于《").append(book.getBookName()).append("》，目前可提供的信息：\n\n");
            reply.append(firstChapterPart.isEmpty() ? "本书简介：" + (book.getBookDesc() != null ? book.getBookDesc() : "暂无") : firstChapterPart);
            reply.append("\n\n");
            reply.append(tocPart.isEmpty() ? "" : tocPart + "\n\n");
            reply.append("💬 您可以问我：\n  • 「主要内容」— 了解小说梗概\n  • 「章节目录」— 查看完整目录\n  • 「介绍一下」— 获取作品简介\n  • 其他问题将在 AI 知识库就绪后获得深度回答");
        }

        return answer(reply.toString());
    }

    /**
     * 章节展示名：章节名已含「第N章」时直接使用，避免重复拼出「第0章 第1章 xxx」
     */
    private String displayChapterName(BookChapter ch) {
        String name = ch.getChapterName() == null ? "" : ch.getChapterName().trim();
        if (name.matches("^第\\s*[0-9一二三四五六七八九十百千零两]+\\s*章.*")) {
            return name;
        }
        return "第" + ch.getChapterNum() + "章 " + name;
    }

    private AiToolRespDto answer(String text) {
        AiToolRespDto resp = new AiToolRespDto();
        resp.setAnswer(text);
        return resp;
    }
}
