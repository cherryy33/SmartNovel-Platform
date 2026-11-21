package com.myproject.novel.controller.front;

import com.myproject.novel.common.req.PageReqDto;
import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.CommentAddReqDto;
import com.myproject.novel.dto.req.ReadingProgressReqDto;
import com.myproject.novel.dto.req.SearchReqDto;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.dto.resp.ChapterContentRespDto;
import com.myproject.novel.dto.resp.ChapterRespDto;
import com.myproject.novel.dto.resp.CommentRespDto;
import com.myproject.novel.service.BookService;
import com.myproject.novel.service.ChapterService;
import com.myproject.novel.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/front/novels")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final ChapterService chapterService;
    private final CommentService commentService;

    @GetMapping
    public RestResp<PageRespDto<BookInfoRespDto>> listBooks(@ModelAttribute SearchReqDto dto) {
        return RestResp.ok(bookService.listPublishedBooks(dto));
    }

    @GetMapping("/{id}")
    public RestResp<BookInfoRespDto> getBookDetail(@PathVariable Long id) {
        return RestResp.ok(bookService.getBookDetail(id));
    }

    @GetMapping("/{bookId}/chapters")
    public RestResp<List<ChapterRespDto>> listChapters(@PathVariable Long bookId) {
        return RestResp.ok(chapterService.listChapters(bookId));
    }

    @GetMapping("/{bookId}/chapters/{chapterId}")
    public RestResp<ChapterContentRespDto> readChapter(@PathVariable Long bookId, @PathVariable Long chapterId) {
        return RestResp.ok(chapterService.readChapter(bookId, chapterId));
    }

    @PostMapping("/{bookId}/progress")
    public RestResp<Void> saveProgress(@PathVariable Long bookId, @RequestBody ReadingProgressReqDto dto) {
        chapterService.saveReadingProgress(bookId, dto);
        return RestResp.ok();
    }

    @GetMapping("/{bookId}/comments")
    public RestResp<PageRespDto<CommentRespDto>> listComments(@PathVariable Long bookId,
            @RequestParam(required = false) Long chapterId, @ModelAttribute PageReqDto page) {
        return RestResp.ok(commentService.listComments(bookId, chapterId, page));
    }

    @PostMapping("/{bookId}/comments")
    public RestResp<Void> addComment(@PathVariable Long bookId, @Valid @RequestBody CommentAddReqDto dto) {
        commentService.addComment(bookId, dto);
        return RestResp.ok();
    }
}