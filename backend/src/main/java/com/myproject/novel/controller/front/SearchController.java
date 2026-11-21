package com.myproject.novel.controller.front;

import com.myproject.novel.common.result.PageRespDto;
import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.SearchReqDto;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/front/search")
@RequiredArgsConstructor
public class SearchController {

    private final BookService bookService;

    @GetMapping
    public RestResp<PageRespDto<BookInfoRespDto>> search(@ModelAttribute SearchReqDto dto) {
        return RestResp.ok(bookService.searchBooks(dto));
    }
}