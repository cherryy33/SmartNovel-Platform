package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.NoticeRespDto;
import com.myproject.novel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/front/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<List<NoticeRespDto>> listNotices() {
        return RestResp.ok(adminService.listActiveNotices());
    }
}