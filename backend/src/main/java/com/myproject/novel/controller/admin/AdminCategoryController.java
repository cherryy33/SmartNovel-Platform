package com.myproject.novel.controller.admin;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.CategoryReqDto;
import com.myproject.novel.dto.resp.CategoryRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<List<CategoryRespDto>> list() {
        return RestResp.ok(adminService.listCategories());
    }

    @PostMapping
    public RestResp<Void> add(@Valid @RequestBody CategoryReqDto dto) {
        adminService.addCategory(dto);
        return RestResp.ok();
    }

    @PutMapping("/{categoryId}")
    public RestResp<Void> update(@PathVariable Long categoryId,
            @Valid @RequestBody CategoryReqDto dto) {
        adminService.updateCategory(categoryId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{categoryId}")
    public RestResp<Void> delete(@PathVariable Long categoryId) {
        adminService.deleteCategory(categoryId);
        return RestResp.ok();
    }
}