package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.BannerReqDto;
import com.myproject.novel.dto.resp.BannerRespDto;
import com.myproject.novel.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<PageRespDto<BannerRespDto>> list(@ModelAttribute PageReqDto page) {
        return RestResp.ok(adminService.listBanners(page));
    }

    @PostMapping
    public RestResp<Void> add(@Valid @RequestBody BannerReqDto dto) {
        adminService.addBanner(dto);
        return RestResp.ok();
    }

    @PutMapping("/{bannerId}")
    public RestResp<Void> update(@PathVariable Long bannerId,
            @Valid @RequestBody BannerReqDto dto) {
        adminService.updateBanner(bannerId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{bannerId}")
    public RestResp<Void> delete(@PathVariable Long bannerId) {
        adminService.deleteBanner(bannerId);
        return RestResp.ok();
    }
}