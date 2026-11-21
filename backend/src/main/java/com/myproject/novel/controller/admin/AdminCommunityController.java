package com.myproject.novel.controller.admin;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.resp.CommunityPostRespDto;
import com.myproject.novel.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminService adminService;

    @GetMapping("/posts")
    public RestResp<PageRespDto<CommunityPostRespDto>> listPosts(
            @ModelAttribute PageReqDto page) {
        return RestResp.ok(adminService.listCommunityPosts(page));
    }

    @DeleteMapping("/posts/{postId}")
    public RestResp<Void> deletePost(@PathVariable Long postId) {
        adminService.deleteCommunityPost(postId);
        return RestResp.ok();
    }

    @DeleteMapping("/replies/{replyId}")
    public RestResp<Void> deleteReply(@PathVariable Long replyId) {
        adminService.deleteCommunityReply(replyId);
        return RestResp.ok();
    }
}