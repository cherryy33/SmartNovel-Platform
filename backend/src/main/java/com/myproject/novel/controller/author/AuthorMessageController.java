package com.myproject.novel.controller.author;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.MessageSendReqDto;
import com.myproject.novel.dto.resp.PrivateMessageRespDto;
import com.myproject.novel.dto.resp.UnreadCountRespDto;
import com.myproject.novel.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/author/messages")
@RequiredArgsConstructor
public class AuthorMessageController {

    private final InteractionService interactionService;

    @GetMapping("/unread-count")
    public RestResp<UnreadCountRespDto> unreadCount() {
        return RestResp.ok(interactionService.getUnreadCount());
    }

    @PostMapping("/reply/{receiverId}")
    public RestResp<Void> reply(@PathVariable Long receiverId,
            @Valid @RequestBody MessageSendReqDto dto) {
        interactionService.sendMessage(receiverId, dto);
        return RestResp.ok();
    }
}