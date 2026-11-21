package com.myproject.novel.controller.author;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.AiExpandReqDto;
import com.myproject.novel.dto.req.AuthorAiAskReqDto;
import com.myproject.novel.dto.resp.AiToolRespDto;
import com.myproject.novel.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/author/ai")
@RequiredArgsConstructor
public class AuthorAiController {

    private final AIService aiService;

    @PostMapping("/ask")
    public RestResp<AiToolRespDto> ask(@Valid @RequestBody AuthorAiAskReqDto dto) {
        return RestResp.ok(aiService.authorAiAsk(dto));
    }

    @PostMapping("/write/{bookId}")
    public RestResp<AiToolRespDto> writeAssist(@PathVariable Long bookId,
            @Valid @RequestBody AiExpandReqDto dto) {
        return RestResp.ok(aiService.writeAssist(bookId, dto));
    }

    /**
     * 流式写作助手：纯文本分块输出，前端边接收边渲染，避免长时间空白等待。
     */
    @PostMapping(value = "/write/{bookId}/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public StreamingResponseBody writeAssistStream(@PathVariable Long bookId,
            @Valid @RequestBody AiExpandReqDto dto) {
        return outputStream -> {
            try {
                aiService.writeAssistStream(bookId, dto, chunk -> {
                    try {
                        outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException e) {
                // 客户端中途关闭连接属于正常操作，静默结束
            }
        };
    }
}