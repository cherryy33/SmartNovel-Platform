package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.ReplyOfficialReqDto;
import com.myproject.novel.dto.resp.MessageBoardRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
public class AdminMessageBoardController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<PageRespDto<MessageBoardRespDto>> list(
            @ModelAttribute PageReqDto page) {
        return RestResp.ok(adminService.listMessageBoard(page));
    }

    @PostMapping("/{messageId}/reply")
    public RestResp<Void> reply(@PathVariable Long messageId,
            @Valid @RequestBody ReplyOfficialReqDto dto) {
        adminService.replyMessageBoard(messageId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{messageId}")
    public RestResp<Void> delete(@PathVariable Long messageId) {
        adminService.deleteMessageBoard(messageId);
        return RestResp.ok();
    }
}