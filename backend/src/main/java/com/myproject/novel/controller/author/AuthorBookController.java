package com.myproject.novel.controller.author;

import com.myproject.novel.common.result.RestResp;
import com.myproject.novel.dto.req.BookAddReqDto;
import com.myproject.novel.dto.req.BookUpdateReqDto;
import com.myproject.novel.dto.resp.BookInfoRespDto;
import com.myproject.novel.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/author/books")
@RequiredArgsConstructor
public class AuthorBookController {

    private final BookService bookService;

    @GetMapping
    public RestResp<List<BookInfoRespDto>> listMyBooks() {
        return RestResp.ok(bookService.listMyBooks());
    }

    @GetMapping("/{bookId}")
    public RestResp<BookInfoRespDto> getBook(@PathVariable Long bookId) {
        return RestResp.ok(bookService.getBookDetail(bookId));
    }

    @PostMapping
    public RestResp<Void> addBook(@Valid @RequestBody BookAddReqDto dto) {
        bookService.addBook(dto);
        return RestResp.ok();
    }

    @PutMapping("/{bookId}")
    public RestResp<Void> updateBook(@PathVariable Long bookId,
            @Valid @RequestBody BookUpdateReqDto dto) {
        bookService.updateBook(bookId, dto);
        return RestResp.ok();
    }

    @DeleteMapping("/{bookId}")
    public RestResp<Void> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return RestResp.ok();
    }
}