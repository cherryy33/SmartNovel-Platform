package com.myproject.novel.controller.author;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.ReplyAddReqDto;
import com.myproject.novel.dto.resp.CommentRespDto;
import com.myproject.novel.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/author/comments")
@RequiredArgsConstructor
public class AuthorCommentController {

    private final CommentService commentService;

    @GetMapping("/{bookId}")
    public RestResp<PageRespDto<CommentRespDto>> listComments(
            @PathVariable Long bookId, @ModelAttribute PageReqDto page) {
        return RestResp.ok(commentService.listComments(bookId, null, page));
    }

    @PostMapping("/{commentId}/reply")
    public RestResp<Void> reply(@PathVariable Long commentId,
            @Valid @RequestBody ReplyAddReqDto dto) {
        commentService.addReply(commentId, dto);
        return RestResp.ok();
    }
}