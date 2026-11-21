package com.myproject.novel.controller.author;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.ChapterAddReqDto;
import com.myproject.novel.dto.req.ChapterUpdateReqDto;
import com.myproject.novel.dto.resp.ChapterRespDto;
import com.myproject.novel.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/author/books/{bookId}/chapters")
@RequiredArgsConstructor
public class AuthorChapterController {

    private final ChapterService chapterService;

    @GetMapping
    public RestResp<List<ChapterRespDto>> listChapters(@PathVariable Long bookId) {
        return RestResp.ok(chapterService.listChapters(bookId));
    }

    /** 章节详情（含正文），供作家端编辑回显 */
    @GetMapping("/{chapterId}")
    public RestResp<ChapterRespDto> getChapter(@PathVariable Long bookId,
            @PathVariable Long chapterId) {
        return RestResp.ok(chapterService.getChapterDetail(bookId, chapterId));
    }

    @PostMapping
    public RestResp<Void> addChapter(@PathVariable Long bookId,
            @Valid @RequestBody ChapterAddReqDto dto) {
        chapterService.addChapter(bookId, dto);
        return RestResp.ok();
    }

    @PutMapping("/{chapterId}")
    public RestResp<Void> updateChapter(@PathVariable Long bookId,
            @PathVariable Long chapterId, @Valid @RequestBody ChapterUpdateReqDto dto) {
        chapterService.updateChapter(bookId, chapterId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{chapterId}")
    public RestResp<Void> deleteChapter(@PathVariable Long bookId,
            @PathVariable Long chapterId) {
        chapterService.deleteChapter(bookId, chapterId);
        return RestResp.ok();
    }
}