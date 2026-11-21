package com.myproject.novel.controller.front;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.PostAddReqDto;
import com.myproject.novel.dto.req.ReplyAddReqDto;
import com.myproject.novel.dto.resp.CommunityPostRespDto;
import com.myproject.novel.dto.resp.CommunityReplyRespDto;
import com.myproject.novel.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public RestResp<PageRespDto<CommunityPostRespDto>> listPosts(@ModelAttribute PageReqDto page) {
        return RestResp.ok(communityService.listPosts(page));
    }

    @GetMapping("/posts/{postId}")
    public RestResp<CommunityPostRespDto> getPost(@PathVariable Long postId) {
        return RestResp.ok(communityService.getPostDetail(postId));
    }

    @PostMapping("/posts")
    public RestResp<Void> addPost(@Valid @RequestBody PostAddReqDto dto) {
        communityService.addPost(dto);
        return RestResp.ok();
    }

    @DeleteMapping("/posts/{postId}")
    public RestResp<Void> deletePost(@PathVariable Long postId) {
        communityService.deletePost(postId);
        return RestResp.ok();
    }

    @GetMapping("/posts/{postId}/replies")
    public RestResp<PageRespDto<CommunityReplyRespDto>> listReplies(
            @PathVariable Long postId, @ModelAttribute PageReqDto page) {
        return RestResp.ok(communityService.listPostReplies(postId, page));
    }

    @PostMapping("/posts/{postId}/replies")
    public RestResp<Void> addReply(@PathVariable Long postId, @Valid @RequestBody ReplyAddReqDto dto) {
        communityService.addPostReply(postId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/replies/{replyId}")
    public RestResp<Void> deleteReply(@PathVariable Long replyId) {
        communityService.deletePostReply(replyId);
        return RestResp.ok();
    }
}