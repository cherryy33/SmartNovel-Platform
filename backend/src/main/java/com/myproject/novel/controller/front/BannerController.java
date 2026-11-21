package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.BannerRespDto;
import com.myproject.novel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/front/banners")
@RequiredArgsConstructor
public class BannerController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<List<BannerRespDto>> listBanners() {
        return RestResp.ok(adminService.listActiveBanners());
    }
}