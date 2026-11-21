package com.myproject.novel.controller.front;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.MessageBoardAddReqDto;
import com.myproject.novel.dto.resp.MessageBoardRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/message-board")
@RequiredArgsConstructor
public class MessageBoardController {

    private final AdminService adminService;

    @PostMapping
    public RestResp<Void> addMessage(@Valid @RequestBody MessageBoardAddReqDto dto) {
        adminService.addMessageBoard(dto);
        return RestResp.ok();
    }

    @GetMapping
    public RestResp<PageRespDto<MessageBoardRespDto>> listMessages(@ModelAttribute PageReqDto page) {
        return RestResp.ok(adminService.listMessageBoardPublic(page));
    }
}