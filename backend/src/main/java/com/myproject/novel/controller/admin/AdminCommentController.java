package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.AuditReqDto;
import com.myproject.novel.dto.resp.CommentRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<PageRespDto<CommentRespDto>> list(
            @ModelAttribute PageReqDto page,
            @RequestParam(required = false) Integer auditStatus) {
        return RestResp.ok(adminService.listComments(page, auditStatus));
    }

    @PostMapping("/{commentId}/audit")
    public RestResp<Void> audit(@PathVariable Long commentId,
            @Valid @RequestBody AuditReqDto dto) {
        adminService.auditComment(commentId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{commentId}")
    public RestResp<Void> delete(@PathVariable Long commentId) {
        adminService.deleteComment(commentId);
        return RestResp.ok();
    }
}