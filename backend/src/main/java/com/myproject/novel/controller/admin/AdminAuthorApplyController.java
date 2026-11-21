package com.myproject.novel.controller.admin;

import com.myproject.novel.common.constant.CommonConsts;
import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.AuditReqDto;
import com.myproject.novel.dto.req.AuthorAuditReqDto;
import com.myproject.novel.dto.resp.AuthorInfoRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 作家入驻审核（管理后台）
 */
@RestController
@RequestMapping("/api/admin/author")
@RequiredArgsConstructor
public class AdminAuthorApplyController {

    private final AdminService adminService;

    /** 入驻申请列表（默认待审核优先） */
    @GetMapping("/applies")
    public RestResp<PageRespDto<AuthorInfoRespDto>> listApplies(
            @ModelAttribute PageReqDto page,
            @RequestParam(required = false) Integer status) {
        return RestResp.ok(adminService.listAuthors(page, status));
    }

    /** 审批：approved=true 通过并升级角色；false 驳回并记录原因 */
    @PostMapping("/apply/{id}/audit")
    public RestResp<Void> audit(@PathVariable Long id,
            @Valid @RequestBody AuthorAuditReqDto dto) {
        AuditReqDto inner = new AuditReqDto();
        inner.setStatus(Boolean.TRUE.equals(dto.getApproved())
                ? CommonConsts.AuthorStatus.APPROVED
                : CommonConsts.AuthorStatus.REJECTED);
        inner.setReviewComment(dto.getRemark());
        adminService.auditAuthor(id, inner);
        return RestResp.ok();
    }
}
