package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.NoticeReqDto;
import com.myproject.novel.dto.resp.NoticeRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<PageRespDto<NoticeRespDto>> list(@ModelAttribute PageReqDto page) {
        return RestResp.ok(adminService.listNotices(page));
    }

    @PostMapping
    public RestResp<Void> add(@Valid @RequestBody NoticeReqDto dto) {
        adminService.addNotice(dto);
        return RestResp.ok();
    }

    @PutMapping("/{noticeId}")
    public RestResp<Void> update(@PathVariable Long noticeId,
            @Valid @RequestBody NoticeReqDto dto) {
        adminService.updateNotice(noticeId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{noticeId}")
    public RestResp<Void> delete(@PathVariable Long noticeId) {
        adminService.deleteNotice(noticeId);
        return RestResp.ok();
    }
}