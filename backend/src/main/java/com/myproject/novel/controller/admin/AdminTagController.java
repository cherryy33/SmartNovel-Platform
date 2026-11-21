package com.myproject.novel.controller.admin;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.TagReqDto;
import com.myproject.novel.dto.resp.TagRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<List<TagRespDto>> list() {
        return RestResp.ok(adminService.listTags());
    }

    @PostMapping
    public RestResp<Void> add(@Valid @RequestBody TagReqDto dto) {
        adminService.addTag(dto);
        return RestResp.ok();
    }

    @PutMapping("/{tagId}")
    public RestResp<Void> update(@PathVariable Long tagId,
            @Valid @RequestBody TagReqDto dto) {
        adminService.updateTag(tagId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{tagId}")
    public RestResp<Void> delete(@PathVariable Long tagId) {
        adminService.deleteTag(tagId);
        return RestResp.ok();
    }
}