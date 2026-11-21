package com.myproject.novel.controller.front;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.ReplyAddReqDto;
import com.myproject.novel.dto.resp.ReplyRespDto;
import com.myproject.novel.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{commentId}/replies")
    public RestResp<PageRespDto<ReplyRespDto>> listReplies(
            @PathVariable Long commentId, @ModelAttribute PageReqDto page) {
        return RestResp.ok(commentService.listReplies(commentId, page));
    }

    @PostMapping("/{commentId}/replies")
    public RestResp<Void> addReply(@PathVariable Long commentId, @Valid @RequestBody ReplyAddReqDto dto) {
        commentService.addReply(commentId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{commentId}")
    public RestResp<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return RestResp.ok();
    }

    @DeleteMapping("/replies/{replyId}")
    public RestResp<Void> deleteReply(@PathVariable Long replyId) {
        commentService.deleteReply(replyId);
        return RestResp.ok();
    }
}