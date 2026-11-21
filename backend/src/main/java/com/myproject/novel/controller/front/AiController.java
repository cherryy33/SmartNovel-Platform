package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.BookAiChatReqDto;
import com.myproject.novel.dto.resp.AiToolRespDto;
import com.myproject.novel.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/ai")
@RequiredArgsConstructor
public class AiController {

    private final AIService aiService;

    @PostMapping("/chat/{bookId}")
    public RestResp<AiToolRespDto> chatWithBook(@PathVariable Long bookId,
            @Valid @RequestBody BookAiChatReqDto dto) {
        return RestResp.ok(aiService.chatWithBook(bookId, dto));
    }
}