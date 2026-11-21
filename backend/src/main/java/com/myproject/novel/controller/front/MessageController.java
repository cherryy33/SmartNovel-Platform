package com.myproject.novel.controller.front;

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

import java.util.List;

@RestController
@RequestMapping("/api/front/messages")
@RequiredArgsConstructor
public class MessageController {

    private final InteractionService interactionService;

    @GetMapping("/contacts")
    public RestResp<List<PrivateMessageRespDto>> listContacts() {
        return RestResp.ok(interactionService.listContacts());
    }

    @GetMapping("/{targetUserId}")
    public RestResp<PageRespDto<PrivateMessageRespDto>> listMessages(
            @PathVariable Long targetUserId, @ModelAttribute PageReqDto page) {
        return RestResp.ok(interactionService.listMessages(targetUserId, page));
    }

    @PostMapping("/send/{receiverId}")
    public RestResp<Void> sendMessage(@PathVariable Long receiverId,
            @Valid @RequestBody MessageSendReqDto dto) {
        interactionService.sendMessage(receiverId, dto);
        return RestResp.ok();
    }

    @GetMapping("/unread-count")
    public RestResp<UnreadCountRespDto> unreadCount() {
        return RestResp.ok(interactionService.getUnreadCount());
    }

    @PutMapping("/{messageId}/read")
    public RestResp<Void> markAsRead(@PathVariable Long messageId) {
        interactionService.markAsRead(messageId);
        return RestResp.ok();
    }
}