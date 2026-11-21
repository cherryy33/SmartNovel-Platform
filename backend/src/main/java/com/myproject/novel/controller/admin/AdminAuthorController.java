package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.AuditReqDto;
import com.myproject.novel.dto.resp.AuthorInfoRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/authors")
@RequiredArgsConstructor
public class AdminAuthorController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<PageRespDto<AuthorInfoRespDto>> list(
            @ModelAttribute PageReqDto page,
            @RequestParam(required = false) Integer status) {
        return RestResp.ok(adminService.listAuthors(page, status));
    }

    @PostMapping("/{authorId}/audit")
    public RestResp<Void> audit(@PathVariable Long authorId,
            @Valid @RequestBody AuditReqDto dto) {
        adminService.auditAuthor(authorId, dto);
        return RestResp.ok();
    }
}