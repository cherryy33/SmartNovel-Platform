package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.UserInfoRespDto;
import com.myproject.novel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public RestResp<PageRespDto<UserInfoRespDto>> list(
            @ModelAttribute PageReqDto page,
            @RequestParam(required = false) String keyword) {
        return RestResp.ok(adminService.listUsers(page, keyword));
    }

    @GetMapping("/{userId}")
    public RestResp<UserInfoRespDto> get(@PathVariable Long userId) {
        return RestResp.ok(adminService.getUser(userId));
    }

    @PutMapping("/{userId}/status")
    public RestResp<Void> updateStatus(@PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {
        adminService.updateUserStatus(userId, body.get("status"));
        return RestResp.ok();
    }

    @PutMapping("/{userId}/vip")
    public RestResp<Void> updateVip(@PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {
        adminService.updateUserVip(userId, body.get("isVip"));
        return RestResp.ok();
    }

    @PutMapping("/{userId}/level")
    public RestResp<Void> updateLevel(@PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {
        adminService.updateUserLevel(userId, body.get("level"));
        return RestResp.ok();
    }
}